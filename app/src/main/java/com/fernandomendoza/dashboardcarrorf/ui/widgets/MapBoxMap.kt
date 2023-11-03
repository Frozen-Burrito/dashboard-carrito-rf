package com.fernandomendoza.dashboardcarrorf.ui.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.viewinterop.NoOpUpdate
import androidx.core.graphics.drawable.toBitmap
import com.fernandomendoza.dashboardcarrorf.R
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager

@Composable
fun MapBoxMap(
    point: Point?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val marker = remember(context) {
        context.getDrawable(R.drawable.toy_car)!!.toBitmap()
    }

    val pointAnnotationManager: MutableState<PointAnnotationManager?> = remember {
        mutableStateOf(null)
    }

    AndroidView(factory = { factoryContext ->
        MapView(factoryContext).also { mapView ->
            mapView.getMapboxMap().loadStyleUri(Style.OUTDOORS)
            val annotationApi = mapView.annotations
            pointAnnotationManager.value = annotationApi.createPointAnnotationManager()
        }
    }, update = { mapView ->
        if (point != null && point.isLatitudeInRange() && point.isLongitudeInRange()) {
            pointAnnotationManager.value?.let {
                it.deleteAll()
                val pointAnnotationOptions =
                    PointAnnotationOptions().withPoint(point).withIconImage(marker)

                it.create(pointAnnotationOptions)
                mapView.getMapboxMap()
                    .flyTo(CameraOptions.Builder().zoom(16.0).center(point).build())
            }
        }
        NoOpUpdate
    }, modifier = modifier
    )
}

fun Point.isLatitudeInRange() = (-90.0 <= latitude() && latitude() <= 90.0)

fun Point.isLongitudeInRange() = (-180.0 <= longitude() && longitude() <= 180.0)

