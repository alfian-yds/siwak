package com.mosque.prayerclock.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mosque.prayerclock.data.repository.HijriDateRepository
import com.mosque.prayerclock.data.repository.SettingsRepository
import com.mosque.prayerclock.server.ServerContent
import com.mosque.prayerclock.server.ServerContentRepository
import com.mosque.prayerclock.server.ServerStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Data untuk tata letak Galeri dan Mutiara Hadits. */
@HiltViewModel
class GalleryViewModel
    @Inject
    constructor(
        contentRepository: ServerContentRepository,
        serverStore: ServerStore,
        private val hijriDateRepository: HijriDateRepository,
        private val settingsRepository: SettingsRepository,
    ) : ViewModel() {
        val content: StateFlow<ServerContent> = contentRepository.content
        val paired: StateFlow<Boolean> = serverStore.state.map { it.isPaired }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

        /** Tanggal Hijriah hari ini (tanggal, bulan, tahun) setelah penyesuaian ± hari. */
        private val _hijri = MutableStateFlow<Triple<Int, Int, Int>?>(null)
        val hijri: StateFlow<Triple<Int, Int, Int>?> = _hijri.asStateFlow()

        init {
            viewModelScope.launch {
                while (true) {
                    runCatching {
                        val h = hijriDateRepository.getCurrentHijriDate()
                        val offset = settingsRepository.getSettings().first().hijriDayOffset
                        _hijri.value = shift(h.day, h.month, h.year, offset)
                    }
                    delay(10 * 60_000L)
                }
            }
        }

        private fun shift(
            day: Int,
            month: Int,
            year: Int,
            offset: Int,
        ): Triple<Int, Int, Int> {
            var d = day + offset
            var m = month
            var y = year
            if (d < 1) {
                d += 30
                m -= 1
                if (m < 1) {
                    m = 12
                    y -= 1
                }
            } else if (d > 30) {
                d -= 30
                m += 1
                if (m > 12) {
                    m = 1
                    y += 1
                }
            }
            return Triple(d, m, y)
        }
    }
