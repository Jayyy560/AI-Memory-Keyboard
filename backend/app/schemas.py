from pydantic import BaseModel
from typing import Optional, List
from datetime import datetime

# Users
class UserCreate(BaseModel):
    device_id: str

class UserResponse(BaseModel):
    id: int
    device_id: str
    
    class Config:
        from_attributes = True

# Contacts
class ContactCreate(BaseModel):
    name: str

class ContactResponse(BaseModel):
    id: int
    name: str
    
    class Config:
        from_attributes = True

# Memories
class MemoryCreate(BaseModel):
    device_id: str
    contact_name: str
    memory_text: str

class MemoryResponse(BaseModel):
    id: int
    user_id: int
    contact_id: int
    memory_text: str
    created_at: datetime
    
    class Config:
        from_attributes = True

# Suggestions
class SuggestionRequest(BaseModel):
    device_id: str
    contact_name: str
    current_topic: str

class SuggestionResponse(BaseModel):
    hint: str
    relevant_memories: List[str]
