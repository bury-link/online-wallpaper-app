package link.bury.onlinewallpaper.data

import androidx.annotation.StringRes
import link.bury.onlinewallpaper.R

/** Curated public image sources. Custom URLs, including personal webcams, stay separate. */
enum class CuratedWallpaperSource(
    val url: String,
    @StringRes val labelRes: Int,
) {
    DWD_RADAR_GERMANY(
        url = "https://www.dwd.de/DWD/wetter/radar/rad_brd_akt.jpg",
        labelRes = R.string.source_dwd_radar_germany,
    ),
    DWD_RADAR_BADEN_WUERTTEMBERG(
        url = "https://www.dwd.de/DWD/wetter/radar/rad_baw_akt.jpg",
        labelRes = R.string.source_dwd_radar_baden_wuerttemberg,
    ),
    CLIMATE_REANALYZER_EUROPE_AFRICA(
        url = "https://climatereanalyzer.org/wx/todays-weather/maps/gfs_euroafr-sat_t2_d1.png",
        labelRes = R.string.source_climate_reanalyzer_europe_africa,
    ),
    NOAA_AURORA_NORTH(
        url = "https://services.swpc.noaa.gov/images/animations/ovation/north/latest.jpg",
        labelRes = R.string.source_noaa_aurora_north,
    ),
    NASA_SDO_193(
        url = "https://sdo.gsfc.nasa.gov/assets/img/latest/latest_1024_0193.jpg",
        labelRes = R.string.source_nasa_sdo_193,
    ),
    NASA_SDO_304(
        url = "https://sdo.gsfc.nasa.gov/assets/img/latest/latest_1024_0304.jpg",
        labelRes = R.string.source_nasa_sdo_304,
    ),
    NASA_SDO_MAGNETOGRAM(
        url = "https://sdo.gsfc.nasa.gov/assets/img/latest/latest_1024_HMIB.jpg",
        labelRes = R.string.source_nasa_sdo_magnetogram,
    ),
    NOAA_GOES16_GEOCOLOR(
        url = "https://cdn.star.nesdis.noaa.gov/GOES16/ABI/CONUS/GEOCOLOR/1250x750.jpg",
        labelRes = R.string.source_noaa_goes16_geocolor,
    ),
    ;

    companion object {
        fun findByUrl(url: String): CuratedWallpaperSource? =
            entries.firstOrNull { it.url == url.trim() }
    }
}
