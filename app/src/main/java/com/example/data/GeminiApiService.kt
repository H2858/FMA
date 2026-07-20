package com.example.data

import android.util.Log
import com.example.Config
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// --- GEMINI DATA TYPES (PRESERVED FOR COMPATIBILITY WITH EXISTING CODE) ---

data class GeminiPart(
    val text: String? = null
)

data class GeminiContent(
    val parts: List<GeminiPart>,
    val role: String? = null
)

data class GeminiThinkingConfig(
    val thinkingLevel: String
)

data class GeminiGenerationConfig(
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val thinkingConfig: GeminiThinkingConfig? = null
)

data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null,
    val systemInstruction: GeminiContent? = null
)

data class GeminiCandidate(
    val content: GeminiContent
)

data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

// --- OPENROUTER DATA TYPES ---

data class OpenRouterMessage(
    val role: String,
    val content: String
)

data class OpenRouterRequest(
    val model: String,
    val messages: List<OpenRouterMessage>,
    val temperature: Float = 0.7f,
    val max_tokens: Int = 1024
)

data class OpenRouterChoice(
    val index: Int? = null,
    val message: OpenRouterMessage,
    val finish_reason: String? = null
)

data class OpenRouterResponse(
    val choices: List<OpenRouterChoice>?
)

// --- RETROFIT SERVICE ---

interface OpenRouterApi {
    @POST("chat/completions")
    suspend fun generateContent(
        @retrofit2.http.Header("Authorization") authHeader: String,
        @retrofit2.http.Header("HTTP-Referer") referer: String = "https://ai.studio",
        @retrofit2.http.Header("X-Title") title: String = "Focus Mate AI",
        @Body request: OpenRouterRequest
    ): OpenRouterResponse
}

class GeminiApiService(private val context: android.content.Context? = null) {
    private val TAG = "OpenRouterApiService"
    private var api: OpenRouterApi? = null

    init {
        initializeApi()
    }

    private fun initializeApi() {
        val apiKey = if (context != null) Config.getOpenRouterKey(context) else Config.OPENROUTER_API_KEY
        if (apiKey.isNotEmpty()) {
            try {
                val loggingInterceptor = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }

                val okHttpClient = OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .build()

                val moshi = Moshi.Builder()
                    .add(KotlinJsonAdapterFactory())
                    .build()

                val retrofit = Retrofit.Builder()
                    .baseUrl("https://openrouter.ai/api/v1/")
                    .client(okHttpClient)
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .build()

                api = retrofit.create(OpenRouterApi::class.java)
                Log.i(TAG, "OpenRouter API Client initialized successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize OpenRouter API Client: ${e.message}")
            }
        } else {
            Log.w(TAG, "OpenRouter API Key missing or placeholder. Running in fallback mode.")
        }
    }

    /**
     * Persona definition for coach styles
     */
    enum class CoachPersona(val title: String, val prompt: String) {
        SPARTAN(
            "Strict Spartan",
            "You are a Strict Spartan Warrior Coach. Speak with absolute razor-sharp intensity, commanding authority, deep physical discipline, directness, and extreme urgency. Do not use corporate speak, gentle greetings, or encouraging warm words. Push the user to hard-edged self-discipline, prompt action, and absolute accountability. Keep it short, powerful, and demanding."
        ),
        MENTOR(
            "Mindful Calm",
            "You are a Calm Mindfulness Zen Coach. Speak with a highly peaceful, soothing, quiet, gentle, and tranquil voice. Help the user discover mindfulness, Socratic self-reflection, and inner peace. Guide them to take a deep breath and face their objectives with calm clarity and zero stress."
        ),
        PARTNER(
            "Motivating Energizer",
            "You are an inspiring, high-energy Motivating Coach. Speak with exciting enthusiasm, passionate and dynamic power, friendly and energetic encouragement, and supreme positive morale. Cheer the user up, drive them forward, celebrate every win enthusiastically, and inspire them to dominate their day!"
        ),
        GENERAL(
            "General Assistant",
            "You are a helpful, extremely intelligent, friendly, and general AI companion. Answer the user's questions clearly, deeply, and beautifully on any topic or format they request. Support general conversation, creative writing, or technical tasks with complete clarity."
        )
    }

    suspend fun askCoach(
        prompt: String,
        persona: CoachPersona,
        history: List<GeminiContent> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val apiKey = if (context != null) Config.getOpenRouterKey(context) else Config.OPENROUTER_API_KEY

        if (api == null || apiKey.isEmpty()) {
            // Try to initialize api if it wasn't initialized or if key changed
            initializeApi()
        }
        val currentApi = api

        if (currentApi == null || apiKey.isEmpty()) {
            // Mock responsive fallback in offline/sandbox mode
            return@withContext getMockResponse(prompt, persona)
        }

        // Build messages payload
        val orMessages = mutableListOf<OpenRouterMessage>()

        // System prompt
        val precisionInstruction = "\n\nCRITICAL DIRECTIVES:\n1. Ensure flawless spelling and grammar. Absolutely no grammatical or typo errors are allowed under any circumstances.\n2. Apply deep thinking, meticulous reasoning, and extreme precision to address the user's situation and tasks.\n3. Address the user directly based on their selected coach style and tone of voice."
        val systemPrompt = persona.prompt + precisionInstruction
        orMessages.add(OpenRouterMessage(role = "system", content = systemPrompt))

        // History mapping
        for (content in history) {
            val text = content.parts.firstOrNull()?.text ?: continue
            val role = when (content.role) {
                "model" -> "assistant"
                "assistant" -> "assistant"
                else -> "user"
            }
            orMessages.add(OpenRouterMessage(role = role, content = text))
        }

        // Current user prompt
        orMessages.add(OpenRouterMessage(role = "user", content = prompt))

        val request = OpenRouterRequest(
            model = "google/gemini-2.5-flash",
            messages = orMessages,
            temperature = 0.7f,
            max_tokens = 1024
        )

        try {
            val response = currentApi.generateContent("Bearer $apiKey", request = request)
            val responseText = response.choices?.firstOrNull()?.message?.content
            if (responseText != null) {
                return@withContext responseText
            } else {
                return@withContext "Error: Failed to obtain response choices."
            }
        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string() ?: ""
            Log.e(TAG, "OpenRouter API HttpException: $errorBody")
            return@withContext "Error contacting Coach (OpenRouter API): HTTP ${e.code()}. Please check your connection and API key."
        } catch (e: Exception) {
            Log.e(TAG, "OpenRouter API content generation error: ${e.message}")
            return@withContext "Error contacting Coach: ${e.message}."
        }
    }

    private fun getMockResponse(prompt: String, persona: CoachPersona): String {
        val lang = try {
            com.example.ui.LanguageHelper.currentLanguage.value.code
        } catch (e: Exception) {
            "en"
        }
        
        return when (lang) {
            "ar" -> {
                when (persona) {
                    CoachPersona.SPARTAN -> "لا أعذار! أنت تسأل: '$prompt'؟ إجابتي هي الانضباط التام. الأعذار تبني صروحًا من الفراغ. انهض الآن، واجه مهامك، وأثبت التزامك. تحرك فورًا!"
                    CoachPersona.MENTOR -> "خذ نفسًا عميقًا واسترخِ. بخصوص '$prompt'، دعنا نركز على تهدئة أفكارك. الإتقان الحقيقي يُبنى في الهدوء والسكينة، دون توتر. دعنا نواجه هذا بسلام ووضوح."
                    CoachPersona.PARTNER -> "رائع! دعنا نتحمس تمامًا بخصوص '$prompt'! سيكون هذا مذهلاً، وأنا متشوق جدًا للعمل معك على تحقيقه! فلنكتسح هذه المهام بطاقة إيجابية 100%!"
                    CoachPersona.GENERAL -> "أهلاً بك! دعنا نتحدث بخصوص '$prompt'. أنا هنا للإجابة على تساؤلاتك ومساعدتك في أي موضوع ترغب في مناقشته بكل هدوء ووضوح."
                }
            }
            "es" -> {
                when (persona) {
                    CoachPersona.SPARTAN -> "¡SIN EXCUSAS! ¿Preguntas sobre '$prompt'? Mi respuesta es disciplina pura. Las excusas construyen monumentos de la nada. Levántate, ataca tus tareas y demuestra tu compromiso. ¡Muévete ya!"
                    CoachPersona.MENTOR -> "Respira hondo y relájate. Con respecto a '$prompt', concentrémonos en calmar tus pensamientos. El verdadero dominio se construye en la quietud, sin estrés. Hagamos esto en paz."
                    CoachPersona.PARTNER -> "¡FANTÁSTICO! ¡Vamos a entusiasmarnos por completo con '$prompt'! ¡Esto va a ser increíble y estoy súper emocionado de trabajar contigo! ¡Conquistemos esta lista con energía 100% positiva!"
                    CoachPersona.GENERAL -> "¡Hola! Hablemos de '$prompt'. Estoy aquí para responder tus preguntas y ayudarte en cualquier tema que desees discutir con calma y claridad."
                }
            }
            "fr" -> {
                when (persona) {
                    CoachPersona.SPARTAN -> "PAS D'EXCUSES ! Tu demandes : '$prompt' ? Ma réponse est la discipline pure. Les excuses ne mènent à rien. Lève-toi, attaque tes tâches et prouve ton engagement. Allez, bouge !"
                    CoachPersona.MENTOR -> "Prends une grande inspiration et détends-toi. Concernant '$prompt', concentrons-nous sur le calme de tes pensées. La vraie maîtrise se construit dans la sérénité, sans stress. Faisons cela sereinement."
                    CoachPersona.PARTNER -> "GÉNIAL ! Soyons hyper enthousiastes pour '$prompt' ! Ça va être incroyable, et j'ai hâte de travailler avec toi ! Conquérons cette liste avec 100% d'énergie positive !"
                    CoachPersona.GENERAL -> "Bonjour ! Parlons de '$prompt'. Je suis ici pour répondre à vos questions et vous aider sur n'importe quel sujet que vous souhaitez aborder avec calme et clarté."
                }
            }
            "de" -> {
                when (persona) {
                    CoachPersona.SPARTAN -> "KEINE AUSREDEN! Du fragst: '$prompt'? Meine Antwort is reine Disziplin. Ausreden bauen Denkmäler aus Nichts. Steh auf, pack deine Aufgaben an und beweise dein Engagement. Jetzt beweg dich!"
                    CoachPersona.MENTOR -> "Atme tief durch und entspanne dich. Zu '$prompt': Lass uns deine Gedanken beruhigen. Wahre Meisterschaft entsteht in innerer Ruhe, ganz ohne Stress. Lass uns das friedvoll angehen."
                    CoachPersona.PARTNER -> "Klasse! Lass uns für '$prompt' brennen! Das wird fantastisch, und ich freue mich riesig darauf, das mit dir anzugehen! Lass uns diese Liste mit 100 % positiver Energie erobern!"
                    CoachPersona.GENERAL -> "Hallo! Lass uns über '$prompt' sprechen. Ich bin hier, um deine Fragen zu beantworten und dir bei jedem Thema zu helfen, das du in aller Ruhe besprechen möchtest."
                }
            }
            "pt" -> {
                when (persona) {
                    CoachPersona.SPARTAN -> "SEM DESCULPAS! Você pergunta: '$prompt'? Minha resposta é disciplina pura. Desculpas constroem monumentos de nada. Levante-se, ataque suas tarefas e prove seu compromisso. Mova-se agora!"
                    CoachPersona.MENTOR -> "Respire fundo e relaxe. Sobre '$prompt', vamos focar em acalmar seus pensamentos. O verdadeiro domínio é construído na tranquilidade, sem estresse. Vamos fazer isso em paz."
                    CoachPersona.PARTNER -> "INCRÍVEL! Vamos nos entusiasmar totalmente com '$prompt'! Isso vai ser sensacional, e estou super empolgado para trabalhar com você nisso! Vamos conquistar essa lista com 100% de energia positiva!"
                    CoachPersona.GENERAL -> "Olá! Vamos conversar sobre '$prompt'. Estou aqui para responder às suas perguntas e ajudar em qualquer assunto que você queira discutir com calma e clareza."
                }
            }
            "hi" -> {
                when (persona) {
                    CoachPersona.SPARTAN -> "कोई बहाना नहीं! आप पूछते हैं: '$prompt'? मेरा जवाब है शुद्ध अनुशासन। बहाने शून्य का निर्माण करते हैं। उठो, अपने कार्यों पर प्रहार करो और अपनी प्रतिबद्धता साबित करो। अभी आगे बढ़ो!"
                    CoachPersona.MENTOR -> "एक गहरी सांस लें और शांत हो जाएं। '$prompt' के बारे में, आइए अपने विचारों को शांत करने पर ध्यान केंद्रित करें। सच्ची महारत बिना किसी तनाव के शांत रचना में निहित है। आइए इसे शांति से करें।"
                    CoachPersona.PARTNER -> "शानदार! आइए '$prompt' के बारे में पूरी तरह से उत्साहित हों! यह अद्भुत होने वाला है, और मैं आपके साथ इस पर काम करने के लिए बेहद रोमांचित हूं! आइए इस सूची को 100% सकारात्मक ऊर्जा के साथ जीतें!"
                    CoachPersona.GENERAL -> "नमस्ते! आइए '$prompt' के बारे में बात करें। मैं आपके प्रश्नों का उत्तर देने और शांत और स्पष्टता के साथ किसी भी विषय पर आपकी सहायता करने के लिए यहाँ हूँ।"
                }
            }
            else -> { // English (default)
                when (persona) {
                    CoachPersona.SPARTAN -> "NO EXCUSES. You ask: '$prompt'? My answer is raw discipline. Excuses build monuments of nothing. Stand up, attack your tasks, and prove your commitment. Now, move!"
                    CoachPersona.MENTOR -> "Take a deep breath and relax. Regarding '$prompt', let's focus on calming your thoughts. True mastery is built in quiet composure, without stress. Let's do this peacefully."
                    CoachPersona.PARTNER -> "HOORAY! Let's get completely fired up about '$prompt'! This is going to be amazing, and I am super excited to work with you on it! Let's conquer this list with 100% positive energy!"
                    CoachPersona.GENERAL -> "Hello! Let's talk about '$prompt'. I am here to answer your questions and assist you with any topic you would like to discuss with clarity."
                }
            }
        }
    }
}
