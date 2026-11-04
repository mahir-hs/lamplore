package app.murmur.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** What Murmur is made of, and who made the pieces. */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        TextButton(onClick = onBack, modifier = Modifier.padding(top = 6.dp)) { Text("Back", color = Ink.dusk) }
        Text("About Murmur", style = MaterialTheme.typography.displayMedium, modifier = Modifier.padding(top = 10.dp))
        Quiet(
            "Audio stories that calm the heart: fables retold from Aesop, folk tales and parables from around the world, " +
                "and original sleep stories. Version 0.4.",
            Modifier.padding(top = 6.dp),
        )

        Heading("Voices and type")
        Quiet("Voices by Piper and Kokoro, running on this phone through sherpa-onnx. Type: Fraunces and Manrope (SIL Open Font License).")

        Heading("Pictures")
        Quiet("Walter Crane, The Baby's Own Aesop (1887), engraved by Edmund Evans. Project Gutenberg #25433. Public domain.")
        Quiet(
            "Online books: Arthur Rackham, Grimm's Fairy Tales (1909), Project Gutenberg #37381, and Beatrix Potter, " +
                "The Tale of Peter Rabbit (1902), Project Gutenberg #14838. Public domain.",
            Modifier.padding(top = 8.dp),
        )

        Heading("Sound")
        Quiet("Music by Kevin MacLeod (incompetech.com), licensed under Creative Commons: By Attribution 4.0.")
        Quiet(
            "Field recordings by kvgarlic, stephan (pdsounds), udo noll, Jarrod Stanley, INNORECORDS, Evillan, " +
                "Guillaume Capsowl Voisin, inchadney, daveincamas and eleanor.l.cooper (CC0, public domain or CC BY). " +
                "Full credits: docs/licenses/SOUNDS.md in the repository.",
            Modifier.padding(top = 8.dp),
        )
        Spacer(Modifier.height(36.dp))
    }
}
