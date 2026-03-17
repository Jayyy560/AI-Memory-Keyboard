import os
from openai import OpenAI
from typing import List

# Use host.docker.internal to access Ollama running on the host machine from inside the docker container
OLLAMA_BASE_URL = os.getenv("OLLAMA_BASE_URL", "http://host.docker.internal:11434/v1")

# The api_key is required by the client but ignored by Ollama
client = OpenAI(
    base_url=OLLAMA_BASE_URL,
    api_key="ollama"
)

class LLMService:
    def __init__(self):
        self.model = "llama3:latest"

    def generate_hint(self, topic: str, contact_name: str, memories: List[str]) -> str:
        """
        Generates a contextual hint using the LLM based on retrieved memories.
        """
        if not memories:
            return "No relevant memories found."

        memories_text = "\n- ".join(memories)
        
        prompt = (
            f"You are an AI assistant helping a user while they type on an Android keyboard.\n"
            f"The user is talking to '{contact_name}' about the topic '{topic}'.\n"
            f"Here are the most relevant past memories the user has saved about {contact_name}:\n"
            f"- {memories_text}\n\n"
            f"Based on these memories, generate a very short, single-sentence conversational hint "
            f"to remind the user of context. Keep it under 10 words. "
            f"Example: 'You promised Sam a book recommendation.'"
        )

        try:
            response = client.chat.completions.create(
                model=self.model,
                messages=[
                    {"role": "system", "content": "You are a concise contextual memory assistant."},
                    {"role": "user", "content": prompt}
                ],
                max_tokens=30,
                temperature=0.7
            )
            hint = response.choices[0].message.content.strip()
            return hint
        except Exception as e:
            print(f"Error calling local LLM: {e}")
            return "Could not generate hint."

# Singleton instance
llm_service = LLMService()
