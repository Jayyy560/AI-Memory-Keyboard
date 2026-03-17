# AI Relationship Memory Keyboard

A privacy-focused Android keyboard powered by AI. It helps you remember context about the people you talk to by analyzing text locally on-device, and surfacing relevant saved memories from a backend Vector Database.

## 1. Project Architecture Diagram

```mermaid
graph TD
    subgraph Mobile Device (Android)
        K[Custom Keyboard UI - Compose]
        IMS[InputMethodService]
        NER[Local ONNX Model - Extractor]
        RC[Retrofit Client]
        
        K <--> IMS
        IMS -->|Extract Entities & Topics| NER
        NER -->|Person & Topic| IMS
        IMS -->|API Requests| RC
    end
    
    subgraph Backend Infrastructure (Docker & Host)
        API[FastAPI Service]
        EMB[Sentence-Transformers]
        LLM[Local Llama 3 via Ollama]
        DB[(PostgreSQL)]
        VEC[(pgvector plugin)]
        
        RC <-->|REST/JSON| API
        API -->|Generate Hint| LLM
        API -->|Generate Vector| EMB
        API <-->|Vector Search| DB
        DB --- VEC
    end
```

## 2. Folder Structure

```
ai-keyboard/
│
├── docker-compose.yml           # Runs Postgres+pgvector and the FastAPI backend
├── README.md                    # This setup guide
│
├── backend/
│   ├── Dockerfile
│   ├── requirements.txt
│   └── app/
│       ├── __init__.py
│       ├── main.py              # FastAPI Application & Endpoints
│       ├── models.py            # SQLAlchemy schema definition
│       ├── schemas.py           # Pydantic validation schemas
│       ├── database.py          # Postgres connection setup
│       └── services/
│           ├── __init__.py
│           ├── embedding_service.py # sentence-transformers (all-MiniLM-L6-v2)
│           └── llm_service.py       # Local Llama 3 hint generation
│
└── mobile/
    ├── build.gradle.kts         # Root Gradle Script
    ├── settings.gradle.kts      # Settings script
    └── app/
        ├── build.gradle.kts     # App level dependencies (Compose, Retrofit, ONNX)
        └── src/main/
            ├── AndroidManifest.xml # Declares the InputMethodService
            ├── res/xml/method.xml  # IME declaration
            └── java/com/example/aikeyboard/
                ├── MemoryKeyboardService.kt # Core Keyboard Service logic
                ├── ui/KeyboardView.kt       # Jetpack Compose Keyboard Layout
                ├── ml/NERExtractor.kt       # ONNX Runtime Entity Extraction
                └── api/BackendClient.kt     # Retrofit Interface
```

## 3. Setup and Deployment Instructions

### Prerequisites
- Docker (including `docker compose`) installed.
- [Ollama](https://ollama.com/) installed on your host machine.
- Android Studio Ladybug or newer.
- Android Emulator or physical device (API 26+).

### Backend Setup (Local / Docker)

1. First, ensure Ollama is running and download the `llama3` model:
   ```bash
   ollama pull llama3:latest
   ollama run llama3:latest
   ```
   *Note: Ollama needs to be accessible on your host machine at `http://localhost:11434`.*

2. Open a terminal and navigate to the project root:
   ```bash
   cd "ai keyboard"
   ```

3. Build and run the backend services using `docker compose`:
   ```bash
   docker compose up --build
   ```
   This will start:
   - PostgreSQL (port `5432`)
   - FastAPI Server (port `8000`)
   - The first run will download the lightweight `all-MiniLM-L6-v2` HuggingFace embedding model into the python container.

4. Check the API is running by visiting:
   - http://localhost:8000/docs (Swagger UI)

### Android App Setup

1. Open the `mobile/` directory using **Android Studio**.
2. Allow Gradle to sync and download all dependencies (Compose, ONNX, Retrofit).
3. If running on an **Emulator**, the API client is configured to hit `http://10.0.2.2:8000` which points to your host machine's localhost. (If physical device, change this IP in `BackendClient.kt` to your laptop's Wi-Fi IP).
4. Build and install the APK (`Run -> Run 'app'`).
5. On the Android device/emulator:
   - Go to **Settings > System > Languages & input > On-screen keyboard**
   - Click **Manage on-screen keyboards** and enable "AI Memory Keyboard".
   - Open any text field (e.g., Messages app) and set the keyboard to the AI Keyboard.

## 4. API Documentation

Exposed at `http://localhost:8000/docs`

### `POST /memory`
*Saves an explicit memory for a contact locally generated on the device.*
- **Body:** `{"device_id": "str", "contact_name": "str", "memory_text": "str"}`
- **Response:** `{"id": int, "user_id": int, ...}`

### `GET /memory/person/{contact_name}?device_id={device_id}`
*Retrieves all raw memory entries stored for a specific person.*
- **Response:** `[{"id": 1, "memory_text": "str", "created_at": "datetime"}]`

### `POST /suggestions`
*Given a detected context, searches Vector DB and generates an LLM hint.*
- **Body:** `{"device_id": "str", "contact_name": "str", "current_topic": "str"}`
- **Response:**
  ```json
  {
      "hint": "You promised Sam a book recommendation.",
      "relevant_memories": ["Sam loves sci-fi books."]
  }
  ```

## 5. Privacy Architecture Details

To meet strict privacy standards:
1. **Never send raw keystrokes:** The system intercepts keystrokes via `MemoryKeyboardService.kt` and maintains an internal buffer.
2. **Local AI Parsing:** Uses `NERExtractor.kt` powered by ONNX to extract the topic and person.
3. **Trigger Driven:** The API ping (`POST /suggestions`) is ONLY sent when a specific entity/topic is matched locally, and only the metadata `(person="Sam", topic="interview")` is sent, NOT the actual text you are typing.
4. **Explicit Memories:** `POST /memory` is only invoked when the user taps "Save Memory" on the keyboard UI.
