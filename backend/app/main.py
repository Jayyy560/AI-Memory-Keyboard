import os
from fastapi import FastAPI, Depends, HTTPException, status
from sqlalchemy.orm import Session
from .database import engine, Base, get_db
from . import models, schemas
from .services.embedding_service import embedding_service
from .services.llm_service import llm_service
from typing import List

from sqlalchemy import text

# Setup pgvector
# For this basic setup, create the extension using raw SQL 
with engine.connect() as conn:
    conn.execute(text('CREATE EXTENSION IF NOT EXISTS vector'))
    conn.commit()

Base.metadata.create_all(bind=engine)

app = FastAPI(title="AI Keyboard Memory API")

def get_or_create_user(db: Session, device_id: str) -> models.User:
    user = db.query(models.User).filter(models.User.device_id == device_id).first()
    if not user:
        user = models.User(device_id=device_id)
        db.add(user)
        db.commit()
        db.refresh(user)
    return user

def get_or_create_contact(db: Session, user_id: int, contact_name: str) -> models.Contact:
    contact = db.query(models.Contact).filter(
        models.Contact.user_id == user_id, 
        models.Contact.name == contact_name
    ).first()
    
    if not contact:
        contact = models.Contact(user_id=user_id, name=contact_name)
        db.add(contact)
        db.commit()
        db.refresh(contact)
    return contact

@app.post("/memory", response_model=schemas.MemoryResponse)
def save_memory(memory_in: schemas.MemoryCreate, db: Session = Depends(get_db)):
    user = get_or_create_user(db, memory_in.device_id)
    contact = get_or_create_contact(db, user.id, memory_in.contact_name)
    
    # Generate embedding for the memory text
    embedding = embedding_service.generate_embedding(memory_in.memory_text)
    
    new_memory = models.Memory(
        user_id=user.id,
        contact_id=contact.id,
        memory_text=memory_in.memory_text,
        embedding=embedding
    )
    db.add(new_memory)
    db.commit()
    db.refresh(new_memory)
    
    return new_memory

@app.get("/memory/person/{contact_name}", response_model=List[schemas.MemoryResponse])
def get_memories_for_person(contact_name: str, device_id: str, db: Session = Depends(get_db)):
    user = db.query(models.User).filter(models.User.device_id == device_id).first()
    if not user:
        return []
        
    contact = db.query(models.Contact).filter(
        models.Contact.user_id == user.id, 
        models.Contact.name == contact_name
    ).first()
    
    if not contact:
        return []
        
    memories = db.query(models.Memory).filter(models.Memory.contact_id == contact.id).all()
    return memories

@app.post("/embedding")
def store_embedding_manually(memory_id: int, db: Session = Depends(get_db)):
    """Utility endpoint to regenerate/store embeddings if needed."""
    memory = db.query(models.Memory).filter(models.Memory.id == memory_id).first()
    if not memory:
        raise HTTPException(status_code=404, detail="Memory not found")
        
    embedding = embedding_service.generate_embedding(memory.memory_text)
    memory.embedding = embedding
    db.commit()
    return {"status": "success", "memory_id": memory.id}

@app.post("/suggestions", response_model=schemas.SuggestionResponse)
def get_suggestions(request: schemas.SuggestionRequest, db: Session = Depends(get_db)):
    user = db.query(models.User).filter(models.User.device_id == request.device_id).first()
    if not user:
        return schemas.SuggestionResponse(hint="No user found.", relevant_memories=[])
        
    contact = db.query(models.Contact).filter(
        models.Contact.user_id == user.id, 
        models.Contact.name == request.contact_name
    ).first()
    
    if not contact:
        return schemas.SuggestionResponse(hint="No context found.", relevant_memories=[])
        
    # Generate embedding for the current topic to search for similar past memories
    topic_emb = embedding_service.generate_embedding(request.current_topic)
    
    # Semantic Search: Find top 3 memories using cosine distance (<=) or L2 distance
    # pgvector operator <-> is L2 distance, <=> is cosine distance, <#> is inner product
    closest_memories = db.query(models.Memory).filter(
        models.Memory.contact_id == contact.id
    ).order_by(
        models.Memory.embedding.cosine_distance(topic_emb)
    ).limit(3).all()
    
    if not closest_memories:
        return schemas.SuggestionResponse(hint="No relevant past memories.", relevant_memories=[])
        
    memory_texts = [m.memory_text for m in closest_memories]
    
    # Generate hint using LLM
    hint = llm_service.generate_hint(
        topic=request.current_topic, 
        contact_name=request.contact_name, 
        memories=memory_texts
    )
    
    return schemas.SuggestionResponse(
        hint=hint,
        relevant_memories=memory_texts
    )

@app.get("/health")
def health_check():
    return {"status": "ok"}
