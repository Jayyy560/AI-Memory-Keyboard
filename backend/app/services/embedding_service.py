from sentence_transformers import SentenceTransformer
from typing import List

# Use a lightweight model suitable for CPU processing
MODEL_NAME = "all-MiniLM-L6-v2"

class EmbeddingService:
    def __init__(self):
        # Load the model once
        self.model = SentenceTransformer(MODEL_NAME)

    def generate_embedding(self, text: str) -> List[float]:
        """
        Generates an embedding for a given text.
        Returns a list of floats (384 dimensions for all-MiniLM-L6-v2).
        """
        embedding = self.model.encode(text)
        return embedding.tolist()

# Singleton instance
embedding_service = EmbeddingService()
