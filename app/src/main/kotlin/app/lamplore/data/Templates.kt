package app.lamplore.data

/**
 * Personalised bedtime stories that work with no internet and no AI: hand-written tales with the
 * listener's name, a chosen animal friend and a pronoun filled in. Verbs after a pronoun are always in
 * the past tense and never "was/were", so they agree with she, he and they.
 */
object Templates {
    enum class Pronoun(val label: String, val they: String, val them: String, val their: String) {
        THEY("They", "they", "them", "their"),
        SHE("She", "she", "her", "her"),
        HE("He", "he", "him", "his"),
    }

    val animals = listOf("fox", "bear", "owl", "rabbit", "turtle", "deer", "otter")

    class Tale(val id: String, val title: String, val blurb: String, val text: String)

    val tales = listOf(
        Tale(
            "lantern", "{name} and the Lantern {Animal}", "A small light, a quiet path, and a hill of stars.",
            """
            Once, when the evening had turned the sky the colour of blueberries, {name} stepped outside to say goodnight to the garden.

            At the gate sat {an}, holding a little lantern. The light was soft and golden, like the inside of a warm cup.

            "I am looking for someone to walk with me," said the {animal}. "The path to the hill is long, and it is easier with company."

            {name} nodded. {They} took the {animal}'s paw, and together they set off.

            They passed the old stone wall, and the wall hummed a slow, sleepy hum. They passed the willow tree, and the willow let its branches sway, left and right, left and right.

            "Is it far?" asked {name}.

            "Not far," said the {animal}. "And there is no hurry. Nothing is waiting for us but the stars."

            So they walked slowly. The lantern glowed. The grass was cool and soft under {their} feet, and every step felt lighter than the one before.

            At last they came to the top of the hill. The {animal} set the lantern down in the grass, and the little flame leaned back and made itself small.

            "Look up," whispered the {animal}.

            {name} looked up. The whole sky was open, deep and wide and kind. One star, then another, then ten, then more than anyone could count, all shining quietly, just for the two of them.

            {name} lay back in the grass. The {animal} lay down close beside {them}. The lantern breathed in and out, in and out.

            "Thank you for walking with me," said the {animal}.

            "Thank you for the light," said {name}.

            The stars blinked slowly. The wind went soft. And {name}'s eyes grew heavy, and heavier, and the whole warm night folded around {them} like a blanket.

            Goodnight, {name}. Goodnight, little light. Goodnight, hill, and goodnight, sky.
            """.trimIndent(),
        ),
        Tale(
            "sleepy", "The Sleepy {Animal} and {name}", "Someone who cannot sleep, and a friend who knows how.",
            """
            In the middle of the forest, where the moss is thick and the paths are narrow, lived {an} who could not fall asleep.

            Every night the {animal} turned this way, and then that way. Every night the moon climbed over the trees, and every night the {animal} was still awake.

            One evening, {name} came wandering through the woods and found the {animal} sitting by a tree, looking tired and a little bit sad.

            "What is the matter?" asked {name}.

            "I cannot sleep," said the {animal}. "I have tried everything."

            {name} sat down beside the {animal}. "May I show you something that works for me?"

            "Please," said the {animal}.

            "First," said {name}, "we let our shoulders drop. Like this. Down, and down, and down."

            The {animal}'s shoulders dropped.

            "Now we breathe in slowly, as if smelling a flower. And out slowly, as if blowing on warm soup."

            In, and out. In, and out.

            "Now we think of the softest thing we know," said {name}. "For me, it is a cloud. A big, quiet cloud that holds me up."

            "For me," said the {animal}, "it is the moss under my paws."

            "Then let us go there," said {name}. "Let the moss be soft. Let it be warm. Let it hold you up, just like a cloud."

            The {animal} yawned, a small one at first, and then a bigger one. {name} yawned too, and soon the two of them were yawning in turn, like a gentle song.

            The forest was quiet. The leaves whispered, hush, hush. The moon looked down and smiled.

            The {animal} curled up on the moss. {name} sat for a while, until the breathing beside {them} was slow and even and deep.

            Then {name} walked home beneath the stars, and {they} too slept, soundly, all night long.
            """.trimIndent(),
        ),
        Tale(
            "boat", "{name}'s Little Boat", "A slow, quiet river and a night that goes by without hurry.",
            """
            There was a little boat tied to a little dock, and in the boat there was a cushion, a blanket, and room for exactly one person.

            That person was {name}.

            {name} climbed in, pulled the blanket up, and untied the rope. The boat gave a small sigh and began to drift.

            The river was wide and slow, and silver with moonlight. It did not ask for anything. It simply carried the boat along.

            First they passed a field of tall grass, and the grass nodded as if to say, goodnight.

            Then they passed a bridge made of old stones. Under the bridge it was cool and dark, and the water whispered a soft, bubbling lullaby, lap, lap, lap.

            "Where are we going?" asked {name} quietly.

            "Wherever the river goes," said the river, in a voice as soft as wind. "There is nothing to do and nowhere to be."

            So {name} stopped trying to know. {They} lay back, and watched the stars move slowly overhead, one by one, as if they too were drifting.

            An owl called from the trees, far off: hoo, hoo. A frog answered, and then another. The crickets kept time.

            The boat rocked, gently, like a hand on a cradle. Each rock was a little slower than the one before.

            The river curved. The moon curved with it. The water went from silver to deep blue, and then to a blue so dark and warm that it felt like velvet.

            {name} closed {their} eyes. The blanket was soft. The boat was steady. The river kept humming.

            And when the little boat came at last to a quiet bay, there was a bed of reeds waiting, and a pillow of cloud above, and all the time in the world to sleep.

            Goodnight, {name}. The river will take care of the rest.
            """.trimIndent(),
        ),
        Tale(
            "garden", "The Garden Where Everyone Rests", "A walk to say goodnight to everything, one by one.",
            """
            At the end of the day, when the light turned honey-coloured, {name} walked into the garden with {an}, to say goodnight.

            "Everything in this garden is getting ready to rest," said the {animal}. "Shall we see?"

            They came first to the sunflowers. All day the sunflowers had followed the sun, and now their big heads bowed down low.

            "Goodnight, sunflowers," said {name}, very softly.

            Then to the roses. Their petals had folded up, soft and tight, like small hands holding a secret.

            "Goodnight, roses."

            They came to the pond, where the lily pads floated side by side, and a little frog sat on the largest, eyes half closed.

            "Goodnight, frog."

            "Croak," said the frog, which means goodnight, in frog.

            They came to the old apple tree. Its leaves rustled once, twice, and went still. The apples hung quietly, rounder and redder in the last of the light.

            "Goodnight, apple tree."

            The bees had already gone home. The butterflies were folded in the leaves. Even the wind lay down in the long grass and stopped.

            "Now it is our turn," said the {animal}.

            {name} yawned. The {animal} yawned too. They walked slowly back toward the house, where a lamp glowed in the window like a friend.

            At the door, {name} turned around one last time. The whole garden was dark and soft and still. And {name} knew that all of it was resting, and being cared for, just as {they} would be.

            {name} climbed into bed. The pillow was cool. The blanket was warm. Somewhere outside, the garden breathed in, and out.

            Goodnight, sunflowers. Goodnight, roses. Goodnight, frog, and tree, and wind.

            Goodnight, {name}.
            """.trimIndent(),
        ),
    )

    /** Fills a tale in. `an` is "a fox" / "an owl". */
    fun fill(tale: Tale, name: String, animal: String, p: Pronoun): Story {
        val n = name.trim()
        val an = (if (animal.first() in "aeiou") "an " else "a ") + animal
        fun sub(t: String) = t
            .replace("{name}", n)
            .replace("{an}", an)
            .replace("{Animal}", animal.replaceFirstChar { it.uppercase() })
            .replace("{animal}", animal)
            .replace("{They}", p.they.replaceFirstChar { it.uppercase() })
            .replace("{they}", p.they)
            .replace("{them}", p.them)
            .replace("{their}", p.their)
        val title = sub(tale.title)
        return app.lamplore.App.addUserStory(title, "Lamplore, for $n", Categories.YOURS, sub(tale.text), Rights.ORIGINAL)
    }
}
