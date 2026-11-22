package uk.ac.dmu.smartcafe

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.Executor
import kotlin.coroutines.resume

data class CafeBranch(val name: String, val lat: Double, val lon: Double)

object DeviceFeatures {

    val branches = listOf(
        CafeBranch("Smart Café – DMU Campus", 52.628, -1.140),
        CafeBranch("Smart Café – Highcross", 52.635, -1.138),
        CafeBranch("Smart Café – Hamilton", 52.656, -1.066)
    )

    suspend fun findNearestBranchEphemeral(context: Context): String {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return "Choose manually"
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return "Choose manually"

        val location = withTimeoutOrNull(10000L) {
            suspendCancellableCoroutine<Location?> { continuation ->
                val cancellationSignal = CancellationSignal()
                continuation.invokeOnCancellation {
                    cancellationSignal.cancel()
                }
                
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        locationManager.getCurrentLocation(
                            LocationManager.NETWORK_PROVIDER,
                            cancellationSignal,
                            context.mainExecutor,
                            { location ->
                                if (continuation.isActive) {
                                    continuation.resume(location)
                                }
                            }
                        )
                    } else {
                        // Fallback for older devices not really requested, but we handle it just in case,
                        // otherwise just resume null since this is ephemeral
                        if (continuation.isActive) {
                             continuation.resume(locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER))
                        }
                    }
                } catch (e: SecurityException) {
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                } catch (e: Exception) {
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            }
        }

        if (location == null) {
            return "Choose manually"
        }

        var nearestBranch: CafeBranch? = null
        var minDistance = Float.MAX_VALUE

        val results = FloatArray(1)
        for (branch in branches) {
            Location.distanceBetween(location.latitude, location.longitude, branch.lat, branch.lon, results)
            if (results[0] < minDistance) {
                minDistance = results[0]
                nearestBranch = branch
            }
        }

        return nearestBranch?.name ?: "Choose manually"
    }
}
