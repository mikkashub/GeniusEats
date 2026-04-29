\# GeniusEats 🌿



> An AI-powered meal planning web app that generates personalized recipes based on your pantry, dietary restrictions, and allergens.



\---



\## What it does



GeniusEats takes what's in your pantry, your dietary preferences, and your allergens and generates a complete tailored recipe using Groq Cloud AI. No generic meals — everything is specific to you.



\---



\## Tech Stack



| Layer | Technology |

|-------|-----------|

| Frontend | HTML, CSS, JavaScript |

| Backend 1 | Spring Boot (port 8080) |

| Backend 2 | Javalin (port 7070) |

| Database 1 | SQLite (geniuseats.db) |

| Database 2 | SQLite (survey.db) |

| AI | Groq Cloud (llama-3.3-70b-versatile) |



\---



\## Prerequisites



\- Java JDK 17+

\- Maven 3.9+

\- A Groq API key from https://console.groq.com



\---



\## Setup



\*\*1 — Clone the repo\*\*

git clone https://github.com/username/GeniusEats.git



\*\*2 — Add your Groq API key\*\*

Copy the example properties file:

cp GeniusEats/GeniusEats/backend/src/main/resources/application.properties.example GeniusEats/GeniusEats/backend/src/main/resources/application.properties

Open it and replace `your\_groq\_api\_key\_here` with your real key from https://console.groq.com



\*\*3 — Start Spring Boot (Terminal 1)\*\*

cd GeniusEats/GeniusEats/backend

mvn spring-boot:run



\*\*4 — Start Javalin (Terminal 2)\*\*

cd demo

mvn compile exec:java "-Dexec.mainClass=com.example.Main"





\*\*5 — Open the app\*\*



Open `src/main/resources/login.html` in your browser.



\---



\## Pages



| Page | Description |

|------|-------------|

| `login.html` | Register and log in |

| `survey.html` | Set dietary preferences and allergens |

| `pantry.html` | Manage your ingredient inventory |

| `home.html` | Generate AI-powered meals |

| `saved.html` | Browse your saved recipe collection |

| `profile.html` | View your preferences and account info |



\---



\## API Endpoints



\*\*Spring Boot (port 8080)\*\*



| Method | Endpoint | Description |

|--------|----------|-------------|

| POST | `/api/auth/register` | Create account |

| POST | `/api/auth/login` | Log in |

| DELETE | `/api/auth/delete/{id}` | Delete account |

| POST | `/api/meals/generate` | Generate a meal |

| PUT | `/api/meals/{id}/save/{userId}` | Toggle save |

| GET | `/api/meals/saved/{userId}` | Get saved meals |

| GET | `/api/meals/current/{userId}` | Get latest meal |



\*\*Javalin (port 7070)\*\*



| Method | Endpoint | Description |

|--------|----------|-------------|

| POST | `/survey` | Create survey |

| GET | `/survey/{id}` | Get survey |

| PUT | `/survey/{id}` | Update survey |

| DELETE | `/survey/{id}` | Delete survey |

| GET | `/pantry/{userId}` | Get pantry |

| POST | `/pantry/{userId}` | Add ingredient |

| DELETE | `/pantry/{userId}/{ingredient}` | Remove ingredient |



\---



\## Notes



\- `application.properties` is excluded from the repo — use `application.properties.example` as a template

\- Both servers must be running at the same time for the app to work fully

\- The app runs locally — no cloud deployment required



