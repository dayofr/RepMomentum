package com.repmomentum.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.repmomentum.AppContainer
import com.repmomentum.MainActivity
import com.repmomentum.RepMomentumApplication
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Parcours principal : favori → poids et reps → enregistrer → visible dans le journal. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class QuickLogFlowTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<RepMomentumApplication>()
        app.container = AppContainer(app, inMemoryDatabase = true)
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() = scenario.close()

    private fun field(tag: String) = compose.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(tag)))

    @Test
    fun logFavoriteSetAndSeeItInJournal() {
        compose.onNodeWithText("Aucune série ce jour").assertExists()
        compose.onNodeWithTag("add_set").performClick()
        compose.onNodeWithText("Développé couché").performClick()
        field("weight_field").performTextReplacement("60")
        field("reps_field").performTextReplacement("8")
        compose.onNodeWithTag("save_set").performClick()

        compose.waitUntilExactlyOneExists(hasText("60 kg × 8 reps"), timeoutMillis = 5_000)
        compose.onNodeWithText("Série enregistrée").assertExists()
        compose.onNodeWithText("Développé couché").assertExists()
    }

    @Test
    fun nextSetIsPrefilledWithLastValues() {
        compose.onNodeWithTag("add_set").performClick()
        compose.onNodeWithText("Tractions").performClick()
        field("weight_field").performTextReplacement("10")
        field("reps_field").performTextReplacement("6")
        compose.onNodeWithTag("save_set").performClick()
        compose.waitUntilExactlyOneExists(hasText("PDC + 10 kg × 6 reps"), timeoutMillis = 5_000)

        compose.onNodeWithTag("add_set").performClick()
        compose.onNodeWithText("Tractions").performClick()
        compose.waitUntilExactlyOneExists(hasText("10") and hasAnyAncestor(hasTestTag("weight_field")), timeoutMillis = 5_000)
        field("reps_field").assert(hasText("6"))
    }

    @Test
    fun unsavedInputSurvivesRecreation() {
        compose.onNodeWithTag("add_set").performClick()
        compose.onNodeWithText("Squat").performClick()
        field("weight_field").performTextReplacement("77,5")
        field("reps_field").performTextReplacement("4")
        scenario.recreate()
        field("weight_field").assert(hasText("77,5"))
        field("reps_field").assert(hasText("4"))
    }

    @Test
    fun invalidInputIsRefused() {
        compose.onNodeWithTag("add_set").performClick()
        compose.onNodeWithText("Squat").performClick()
        field("weight_field").performTextReplacement("")
        field("reps_field").performTextReplacement("")
        compose.onNodeWithTag("save_set").performClick()
        // Charge et répétitions manquantes : deux messages.
        compose.onAllNodesWithText("Champ requis", useUnmergedTree = true).assertCountEquals(2)
        compose.onNodeWithTag("save_set").assertExists()
    }
}
