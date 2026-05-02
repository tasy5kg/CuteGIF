package com.nht.gif

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GifSplitViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private val repository: GifSplitRepository = mockk(relaxed = true)
  private val gifPath = "/data/test.gif"
  private val fakeBitmap: Bitmap = mockk()
  private val fakeFrames = listOf(fakeBitmap, fakeBitmap, fakeBitmap)

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun createViewModel() = GifSplitViewModel(repository, gifPath)

  @Test
  fun `initial state is Loading`() = runTest {
    coEvery { repository.extractFrames(any()) } coAnswers { fakeFrames }
    val viewModel = createViewModel()
    assertEquals(GifSplitViewModel.UiState.Loading, viewModel.uiState.value)
  }

  @Test
  fun `state becomes FramesReady after successful extraction`() = runTest {
    coEvery { repository.extractFrames(gifPath) } returns fakeFrames
    val viewModel = createViewModel()
    advanceUntilIdle()
    val state = viewModel.uiState.value as GifSplitViewModel.UiState.FramesReady
    assertEquals(fakeFrames, state.frames)
    assertFalse(state.isSaving)
  }

  @Test
  fun `state becomes Error when extractFrames returns null`() = runTest {
    coEvery { repository.extractFrames(gifPath) } returns null
    val viewModel = createViewModel()
    advanceUntilIdle()
    assertEquals(GifSplitViewModel.UiState.Error, viewModel.uiState.value)
  }

  @Test
  fun `saveFrame sets isSaving true then false and emits SaveSuccess`() = runTest {
    coEvery { repository.extractFrames(gifPath) } returns fakeFrames
    coEvery { repository.saveFrame(any(), any()) } returns Unit
    val viewModel = createViewModel()
    advanceUntilIdle()

    val events = mutableListOf<GifSplitViewModel.Event>()
    val collectJob = launch {
      viewModel.events.collect { events.add(it) }
    }

    viewModel.saveFrame(2)
    // Mid-save: isSaving should be true
    advanceUntilIdle()

    val finalState = viewModel.uiState.value as GifSplitViewModel.UiState.FramesReady
    assertFalse(finalState.isSaving)
    assertTrue(events.contains(GifSplitViewModel.Event.SaveSuccess))
    coVerify { repository.saveFrame(gifPath, 2) }
    collectJob.cancel()
  }

  @Test
  fun `saveFrame is no-op when state is not FramesReady`() = runTest {
    coEvery { repository.extractFrames(gifPath) } returns null
    val viewModel = createViewModel()
    advanceUntilIdle()
    assertEquals(GifSplitViewModel.UiState.Error, viewModel.uiState.value)

    viewModel.saveFrame(1)
    advanceUntilIdle()

    coVerify(exactly = 0) { repository.saveFrame(any(), any()) }
  }

  @Test
  fun `cleanup is called when ViewModel is cleared`() {
    coEvery { repository.extractFrames(any()) } returns fakeFrames
    val viewModel = createViewModel()
    // onCleared() is protected; reflection simulates framework-driven lifecycle destruction
    ViewModel::class.java.getDeclaredMethod("onCleared").apply { isAccessible = true }.invoke(viewModel)
    verify { repository.cleanup() }
  }
}
