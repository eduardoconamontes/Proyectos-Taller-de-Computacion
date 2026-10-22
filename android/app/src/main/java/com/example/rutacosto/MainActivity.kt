package com.example.rutacosto

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.rutacosto.ui.theme.RutaCostoTheme
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.text.NumberFormat
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            RutaCostoTheme {
                RutaCostoApp()
            }
        }
    }
}

private const val URL_SERVIDOR = "http://34.233.220.221:5000"
private val clienteHttp = OkHttpClient()
private val hiloPrincipal = Handler(Looper.getMainLooper())

@Composable
fun RutaCostoApp() {
    var sesionIniciada by remember { mutableStateOf(false) }
    var usuarioActual by remember { mutableStateOf("") }

    if (sesionIniciada) {
        CalculadoraRutaCosto(
            usuarioActual = usuarioActual,
            onCerrarSesion = {
                sesionIniciada = false
                usuarioActual = ""
            }
        )
    } else {
        PantallaInicioSesion(
            onAccesoCorrecto = { login ->
                usuarioActual = login
                sesionIniciada = true
            }
        )
    }
}

@Composable
fun PantallaInicioSesion(onAccesoCorrecto: (String) -> Unit) {
    var usuario by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("") }
    var validando by remember { mutableStateOf(false) }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "🚗 RutaCosto",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Inicio de sesión",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Ingresa tus credenciales para acceder a la calculadora de viajes.",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = usuario,
                onValueChange = { usuario = it },
                label = { Text("Usuario") },
                singleLine = true,
                enabled = !validando,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = contrasena,
                onValueChange = { contrasena = it },
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                enabled = !validando,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val login = usuario.trim()
                    if (login.isEmpty() || contrasena.isEmpty()) {
                        mensaje = "Ingresa usuario y contraseña."
                    } else {
                        validando = true
                        mensaje = "Validando con el servidor Flask..."

                        val json = JSONObject()
                            .put("login", login)
                            .put("password", contrasena)
                        val cuerpo = json.toString().toRequestBody(
                            "application/json; charset=utf-8".toMediaType()
                        )
                        val solicitud = Request.Builder()
                            .url("$URL_SERVIDOR/login")
                            .post(cuerpo)
                            .build()

                        clienteHttp.newCall(solicitud).enqueue(object : Callback {
                            override fun onFailure(call: Call, e: IOException) {
                                hiloPrincipal.post {
                                    validando = false
                                    mensaje = "No se pudo conectar. Revisa AWS y Flask."
                                }
                            }

                            override fun onResponse(call: Call, response: Response) {
                                response.use {
                                    try {
                                        val contenido = response.body?.string().orEmpty()
                                        val jsonRespuesta = JSONObject(contenido)
                                        val valido = response.isSuccessful &&
                                                jsonRespuesta.optBoolean("valido", false)
                                        val mensajeServidor = jsonRespuesta.optString(
                                            "mensaje", "Credenciales incorrectas"
                                        )
                                        hiloPrincipal.post {
                                            validando = false
                                            if (valido) {
                                                contrasena = ""
                                                onAccesoCorrecto(login)
                                            } else {
                                                mensaje = mensajeServidor
                                            }
                                        }
                                    } catch (e: Exception) {
                                        hiloPrincipal.post {
                                            validando = false
                                            mensaje = "Error al procesar la respuesta de Flask."
                                        }
                                    }
                                }
                            }
                        })
                    }
                },
                enabled = !validando,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (validando) "VALIDANDO..." else "INICIAR SESIÓN (POST)")
            }

            if (mensaje.isNotEmpty()) {
                Text(
                    text = mensaje,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun ConsultarUsuarioPanel() {
    var nombre by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var rut by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("Presiona el botón para consultar AWS.") }
    var consultando by remember { mutableStateOf(false) }

    Text(
        text = "Datos de usuario - Flask",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )

    Text(
        text = "Consulta la información del usuario desde AWS EC2.",
        style = MaterialTheme.typography.bodyMedium
    )

    Button(
        onClick = {
            consultando = true
            mensaje = "Consultando usuario..."
            nombre = ""
            correo = ""
            rut = ""

            val solicitud = Request.Builder()
                .url("$URL_SERVIDOR/usuario")
                .get()
                .build()

            clienteHttp.newCall(solicitud).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    hiloPrincipal.post {
                        consultando = false
                        mensaje = "No se pudo conectar con AWS. Revisa Flask."
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        try {
                            if (!response.isSuccessful) {
                                val codigo = response.code
                                hiloPrincipal.post {
                                    consultando = false
                                    mensaje = "Error HTTP $codigo"
                                }
                            } else {
                                val datos = JSONObject(response.body?.string().orEmpty())
                                val nombreRecibido = datos.getString("nombre")
                                val correoRecibido = datos.getString("correo")
                                val rutRecibido = datos.getString("rut")
                                hiloPrincipal.post {
                                    nombre = nombreRecibido
                                    correo = correoRecibido
                                    rut = rutRecibido
                                    consultando = false
                                    mensaje = "Consulta GET realizada correctamente."
                                }
                            }
                        } catch (e: Exception) {
                            hiloPrincipal.post {
                                consultando = false
                                mensaje = "No se pudo procesar el JSON del servidor."
                            }
                        }
                    }
                }
            })
        },
        enabled = !consultando,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(if (consultando) "CONSULTANDO..." else "CONSULTAR USUARIO (GET)")
    }

    Text(mensaje, style = MaterialTheme.typography.bodyMedium)

    if (nombre.isNotEmpty()) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Nombre: $nombre")
                Text("Correo: $correo")
                Text("RUT: $rut")
            }
        }
    }
}

data class ResultadoViaje(
    val litrosNecesarios: Double,
    val costoCombustible: Double,
    val costoTotal: Double,
    val costoPorPersona: Double
)

@Composable
fun CalculadoraRutaCosto(usuarioActual: String, onCerrarSesion: () -> Unit) {

    var distancia by remember { mutableStateOf("") }
    var rendimiento by remember { mutableStateOf("") }
    var precioCombustible by remember { mutableStateOf("") }
    var peajes by remember { mutableStateOf("") }
    var personas by remember { mutableStateOf("") }

    var resultado by remember {
        mutableStateOf<ResultadoViaje?>(null)
    }

    var mensajeError by remember {
        mutableStateOf("")
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = "🚗 RutaCosto",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Calcula cuánto costará tu viaje y cuánto deberá pagar cada persona.",
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "Sesión iniciada: $usuarioActual",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedButton(
                onClick = onCerrarSesion,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("CERRAR SESIÓN")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = distancia,
                onValueChange = { distancia = it },
                label = {
                    Text("Distancia del viaje (km)")
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = rendimiento,
                onValueChange = { rendimiento = it },
                label = {
                    Text("Rendimiento del vehículo (km/L)")
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = precioCombustible,
                onValueChange = { precioCombustible = it },
                label = {
                    Text("Precio combustible por litro ($)")
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = peajes,
                onValueChange = { peajes = it },
                label = {
                    Text("Costo de peajes ($)")
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = personas,
                onValueChange = { personas = it },
                label = {
                    Text("Cantidad de personas")
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {

                    val distanciaNumero =
                        distancia.replace(",", ".").toDoubleOrNull()

                    val rendimientoNumero =
                        rendimiento.replace(",", ".").toDoubleOrNull()

                    val precioNumero =
                        precioCombustible.toDoubleOrNull()

                    val peajesNumero =
                        peajes.toDoubleOrNull()

                    val personasNumero =
                        personas.toIntOrNull()

                    if (
                        distanciaNumero == null ||
                        distanciaNumero <= 0
                    ) {

                        mensajeError =
                            "Ingresa una distancia válida."

                        resultado = null

                    } else if (
                        rendimientoNumero == null ||
                        rendimientoNumero <= 0
                    ) {

                        mensajeError =
                            "Ingresa un rendimiento válido."

                        resultado = null

                    } else if (
                        precioNumero == null ||
                        precioNumero <= 0
                    ) {

                        mensajeError =
                            "Ingresa un precio de combustible válido."

                        resultado = null

                    } else if (
                        peajesNumero == null ||
                        peajesNumero < 0
                    ) {

                        mensajeError =
                            "Ingresa un costo de peajes válido."

                        resultado = null

                    } else if (
                        personasNumero == null ||
                        personasNumero <= 0
                    ) {

                        mensajeError =
                            "La cantidad de personas debe ser mayor que 0."

                        resultado = null

                    } else {

                        resultado = calcularViaje(
                            distanciaNumero,
                            rendimientoNumero,
                            precioNumero,
                            peajesNumero,
                            personasNumero
                        )

                        mensajeError = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {

                Text("CALCULAR VIAJE")
            }

            OutlinedButton(
                onClick = {

                    distancia = ""
                    rendimiento = ""
                    precioCombustible = ""
                    peajes = ""
                    personas = ""

                    resultado = null
                    mensajeError = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {

                Text("LIMPIAR")
            }

            if (mensajeError.isNotEmpty()) {

                Text(
                    text = "⚠️ $mensajeError",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }

            resultado?.let {

                ResultadoViajeCard(it)
            }

            Spacer(modifier = Modifier.height(16.dp))
            ConsultarUsuarioPanel()
        }
    }
}

fun calcularViaje(
    distancia: Double,
    rendimiento: Double,
    precioCombustible: Double,
    peajes: Double,
    personas: Int
): ResultadoViaje {

    val litrosNecesarios =
        distancia / rendimiento

    val costoCombustible =
        litrosNecesarios * precioCombustible

    val costoTotal =
        costoCombustible + peajes

    val costoPorPersona =
        costoTotal / personas

    return ResultadoViaje(
        litrosNecesarios,
        costoCombustible,
        costoTotal,
        costoPorPersona
    )
}

@Composable
fun ResultadoViajeCard(
    resultado: ResultadoViaje
) {

    val formatoPesos =
        NumberFormat.getCurrencyInstance(
            Locale("es", "CL")
        )

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Text(
                text = "✅ Resultado del viaje",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "⛽ Combustible necesario: " +
                        String.format(
                            Locale("es", "CL"),
                            "%.2f litros",
                            resultado.litrosNecesarios
                        )
            )

            Text(
                text = "Costo de combustible: " +
                        formatoPesos.format(
                            resultado.costoCombustible
                        )
            )

            Text(
                text = "Costo total del viaje: " +
                        formatoPesos.format(
                            resultado.costoTotal
                        )
            )

            Text(
                text = "👥 Costo por persona: " +
                        formatoPesos.format(
                            resultado.costoPorPersona
                        ),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

