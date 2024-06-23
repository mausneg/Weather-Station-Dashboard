package com.example.iot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.iot.firebase.model.CurrentData
import com.example.iot.firebase.model.WeatherData
import com.example.iot.ui.theme.IoTTheme
import com.google.accompanist.pager.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    private val viewModel: WeatherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IoTTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun StarryBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val starPaint = Paint().apply {
            color = Color(android.graphics.Color.WHITE)
            isAntiAlias = true
        }

        // Generate random stars
        val random = java.util.Random()
        for (i in 0 until 100) { // Adjust the number of stars here
            val x = random.nextFloat() * canvasWidth
            val y = random.nextFloat() * canvasHeight / 2 // Only draw on the upper half
            drawCircle(
                color = Color.White,
                radius = random.nextFloat() * 3f, // Random size for stars
                center = Offset(x, y)
            )
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun MainScreen(modifier: Modifier = Modifier, viewModel: WeatherViewModel) {
    var isLedOn by remember { mutableStateOf(false) }
    val currentData by viewModel.currentData.collectAsState()
    val forecastingData by viewModel.forecastingData.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var showContent by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.fetchInitialLedState { ledState ->
            isLedOn = ledState
        }
    }


    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black,
                        Color(0xFF2C90C7),
                    )
                )
            )
            .padding(16.dp)
    ) {
        StarryBackground(modifier = Modifier.fillMaxSize())

        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Power: ",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Switch(
                        checked = isLedOn, onCheckedChange = { checked ->
                            isLedOn = checked
                            viewModel.updateLedState(checked)
                        }, colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Green, uncheckedThumbColor = Color.Red
                        )
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "${currentData.temperature}°C",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                ImageSection(weatherName = currentData.weather_name)
            }
            item {
                Text(
                    text = formatWeatherName(currentData.weather_name),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Date: ${currentData.timestamp}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                ItemSection(currentData = currentData)
                Spacer(modifier = Modifier.height(24.dp))
            }
            item {
                Text(
                    text = "Forecast",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
            item {
                val limitedForecastingData = forecastingData.takeLast(5).reversed()
                val pagerState = rememberPagerState()

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    HorizontalPager(
                        count = limitedForecastingData.size,
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) { page ->
                        val data = limitedForecastingData[page]
                        AnimatedVisibility(
                            visible = showContent,
                            enter = slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }) + fadeIn(),
                            exit = slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth }) + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Date: ${data.timestamp}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                WeatherHourlyForecast(data)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }

                    HorizontalPagerIndicator(
                        pagerState = pagerState,
                        modifier = Modifier
                            .padding(16.dp),
                    )

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    showContent = false
                                    delay(300)
                                    val prevPage = (pagerState.currentPage - 1).coerceAtLeast(0)
                                    pagerState.animateScrollToPage(
                                        page = prevPage,
                                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                                    )
                                    showContent = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                Color(0xFF1D5C7E),
                                Color(0xFFFFFFFF),
                            )
                        ) {
                            Text("Previous")
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    showContent = false
                                    delay(300)
                                    val nextPage = (pagerState.currentPage + 1).coerceAtMost(limitedForecastingData.size - 1)
                                    pagerState.animateScrollToPage(
                                        page = nextPage,
                                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                                    )
                                    showContent = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                Color(0xFF1D5C7E),
                                Color(0xFFFFFFFF),
                            )
                        ) {
                            Text("Next")
                        }

                    }
                }

            }
        }
    }
}

@Composable
fun ItemSection(currentData: CurrentData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(vertical = 16.dp)
            .background(Color(0x37FFFFFF), shape = RoundedCornerShape(8.dp)),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ForecastingDetail(title = "Humidity", value = "${currentData.humidity}%")
        VerticalDivider()
        ForecastingDetail(title = "Light", value = currentData.light.toString())
        VerticalDivider()
        ForecastingDetail(title = "Rainfall", value = "${currentData.rainfall}")
    }
}
@Composable
fun WeatherHourlyForecast(data: WeatherData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(vertical = 16.dp)
            .background(Color(0x37FFFFFF), shape = RoundedCornerShape(8.dp)),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ForecastingDetail(title = "Humidity", value = "${data.humidity}%")
        VerticalDivider()
        ForecastingDetail(title = "Light", value = data.light.toString())
        VerticalDivider()
        ForecastingDetail(title = "Rainfall", value = "${data.rainfall}")
        VerticalDivider()
        ForecastingDetail(title = "Temperature", value = "${data.temperature}°C")
    }
}
@Composable
fun VerticalDivider() {
    Divider(
        color = Color.White,
        modifier = Modifier
            .fillMaxHeight()
            .width(1.dp)
            .padding(vertical = 8.dp)
    )
}

@Composable
fun ForecastingDetail(title: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ImageSection(weatherName: String) {
    val imageRes = when (weatherName.lowercase()) {
        "light_rain" -> R.drawable.ic_rainy
        "heavy_rain" -> R.drawable.ic_heavyrain
        "cloudy" -> R.drawable.ic_cloudly
        "sunny" -> R.drawable.ic_sunny
        "overcast" -> R.drawable.ic_overcast
        "drizzle" -> R.drawable.drizzle
//        "hot_sunny" -> R.drawable.hot_sunny
//        "mild" -> R.drawable.ic_sunny
//        "light_rain_night" -> R.drawable.heavy_night_rain
//        "cloudy_night" -> R.drawable.cloudy_night
//        "clear_night" -> R.drawable.clear_night
//        "partly_cloudy_night" -> R.drawable.partly_cloudy_night
//        "drizzle_night" -> R.drawable.drizzle_night
//        "warm_clear_night" -> R.drawable.clear_night
//        "unknown" -> R.drawable.unknown
        else -> R.drawable.ic_sunny
    }
    Box(modifier = Modifier.size(200.dp)) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = "Weather Icon",
            modifier = Modifier.fillMaxSize()
        )
    }
}


fun formatWeatherName(weatherName: String): String {
    return weatherName.split("_").joinToString(" ") { it.capitalize() }
}

@Preview(showBackground = true)
@Composable
fun PreviewWeatherUI() {
    IoTTheme {
        MainScreen(viewModel = WeatherViewModel())
    }
}
