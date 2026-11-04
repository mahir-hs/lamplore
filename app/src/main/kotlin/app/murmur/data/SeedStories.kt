package app.murmur.data

object SeedStories {
    val all: List<Story> by lazy { base + MoreStories.all }

    private val base: List<Story> = listOf(
        Story(
            id = "seed_tortoise_hare",
            plates = listOf(Plate("crane_hare_tortoise", Books.CRANE, .62f, .18f, 1f, .5f)),
            motif = Motif.TORTOISE_HARE,
            scene = app.murmur.audio.AmbienceKind.FOREST,
            title = "The Tortoise and the Hare",
            author = "Aesop, retold",
            category = Categories.FABLES,
            moods = setOf("patience", "learn", "calm"),
            summary = "Slow and steady wins when pride falls asleep.",
            rights = Rights.PUBLIC_DOMAIN,
            body = """
                Long ago, in a meadow where the grass grew thick and the sky was wide, there lived a hare who was very proud of his speed. Every morning he stretched his long legs and told anyone who would listen, "No one in this meadow can run as fast as I can."

                One day a tortoise, who had been walking quietly along the path, said softly, "I will race you."

                The animals laughed. The hare laughed loudest of all. But the tortoise only nodded, and the race was set. The fox raised his paw and called out, "Ready, steady, go!"

                The hare shot forward like an arrow and was soon out of sight. The tortoise took one step, and then another, and then another. He did not look at the clouds, and he did not look at the crowd. He looked only at the path ahead.

                Halfway along the track, the hare stopped. He looked back and saw nothing but dust and empty road. "There is plenty of time," he yawned. "I will rest beneath this tree." And the warm shade made his eyes heavy, and soon he was fast asleep.

                The tortoise kept walking. Step by step, he passed the sleeping hare. Step by step, he climbed the last hill.

                When the hare finally woke, the sun was low. He sprang up and ran with all his strength, but as he came around the bend, he saw the tortoise crossing the finish line while the animals cheered.

                The hare hung his head. The tortoise smiled kindly and said, "Speed is a gift, my friend. But steady effort, day after day, takes you farther."

                And from that day on, the hare never boasted again, and the meadow remembered that patience and steady effort are quiet powers.
            """.trimIndent(),
        ),
        Story(
            id = "seed_lion_mouse",
            plates = listOf(Plate("crane_mouse_lion", Books.CRANE, 0f, .5f, .52f, 1f)),
            motif = Motif.LION,
            scene = app.murmur.audio.AmbienceKind.DESERT,
            title = "The Lion and the Mouse",
            author = "Aesop, retold",
            category = Categories.FABLES,
            moods = setOf("inspire", "learn"),
            summary = "No act of kindness is ever too small.",
            rights = Rights.PUBLIC_DOMAIN,
            body = """
                A great lion lay sleeping in the shade of an old tree. A tiny mouse, not looking where she was going, ran right across his nose.

                The lion woke with a roar and trapped her beneath his huge paw. "How dare you wake the king of the forest!" he thundered.

                The mouse trembled. "Please, great king, let me go. One day I may be able to help you."

                The lion laughed so hard that his paw lifted. "You? Help me? What could a mouse possibly do?" But something in her small brave voice touched him, and he let her go.

                Some days later, the lion walked into a hunter's trap. Thick ropes closed around him, and no matter how he struggled and roared, he could not break free.

                The mouse heard his roar from far away and came running. "Be still, great king," she said. And she began to gnaw at the ropes with her tiny teeth. Strand by strand, rope by rope, the net loosened, until at last the lion stepped out free.

                "I laughed at you," he said quietly. "And yet you saved me."

                The mouse smiled. "Kindness is never wasted, no matter how small the one who gives it, or how small the one who receives."

                From that day, the lion and the mouse were friends, and the forest learned that gentleness can return in the most unexpected ways.
            """.trimIndent(),
        ),
        Story(
            id = "seed_crow_pitcher",
            plates = listOf(Plate("crane_crow_pitcher", Books.CRANE, .02f, .02f, .42f, .38f)),
            motif = Motif.CROW_PITCHER,
            scene = app.murmur.audio.AmbienceKind.DESERT,
            title = "The Crow and the Pitcher",
            author = "Aesop, retold",
            category = Categories.FABLES,
            moods = setOf("patience", "learn", "inspire"),
            summary = "Where force fails, a patient mind finds a way.",
            rights = Rights.PUBLIC_DOMAIN,
            body = """
                It was the hottest summer anyone could remember. The rivers had shrunk to cracked mud, and a thirsty crow flew for hours looking for water.

                At last, in a garden, he saw a tall pitcher. He landed on its rim and peered inside. There was water, but only a little, far down at the bottom, and his beak could not reach it.

                He pushed against the pitcher with his wings, but it was too heavy to tip. He tried to break it with his beak, but the clay was too strong. He sat on the rim, tired and dry, and for a moment he felt like giving up.

                Then he looked at the ground. There were small pebbles scattered all around. A thought came to him, slow and bright like the sunrise.

                He picked up one pebble and dropped it into the pitcher. Plink. The water rose, just a little. He fetched another pebble. Plink. Then another. And another. Each one was small, and each one took time, but the water crept higher and higher.

                At last the water reached the top, and the crow drank deeply, until his thirst was gone.

                He rose into the evening sky, quiet and content, knowing that a clear mind and patient effort can solve what strength alone never could.
            """.trimIndent(),
        ),
        Story(
            id = "seed_wind_sun",
            plates = listOf(Plate("crane_wind_sun", Books.CRANE, .45f, 0f, 1f, .42f)),
            motif = Motif.SUN_WIND,
            scene = app.murmur.audio.AmbienceKind.WIND,
            title = "The North Wind and the Sun",
            author = "Aesop, retold",
            category = Categories.FABLES,
            moods = setOf("calm", "patience", "learn"),
            summary = "Gentleness opens what force only closes.",
            rights = Rights.PUBLIC_DOMAIN,
            body = """
                One day the North Wind and the Sun argued about which of them was stronger. Just then, a traveler walked along the road below, wrapped in a thick cloak.

                "Let us settle this," said the Sun. "Whoever can make that traveler take off his cloak is the stronger."

                "Easily done," said the North Wind, and he drew a great breath and blew. The traveler gasped and pulled his cloak tighter around his shoulders. The harder the wind blew, the tighter the traveler held on.

                At last the wind grew tired and gave up. "Your turn," he said.

                The Sun said nothing at all. She simply shone, warm and steady. The traveler felt the chill leave his bones. He loosened his cloak. As the warmth grew, he wiped his forehead, and soon he took off the cloak and carried it over his arm.

                The North Wind learned that day what the Sun already knew: that kindness and warmth can do what force never can.
            """.trimIndent(),
        ),
        Story(
            id = "seed_stonecutter",
            motif = Motif.MOUNTAIN,
            scene = app.murmur.audio.AmbienceKind.STREAM,
            title = "The Stonecutter's Wish",
            author = "Japanese folktale, retold",
            category = Categories.WISDOM,
            moods = setOf("inspire", "learn", "calm"),
            summary = "A man who kept wishing to be something more, and found what he already was.",
            rights = Rights.PUBLIC_DOMAIN,
            body = """
                At the foot of a great mountain lived a stonecutter named Tarou. Every morning he climbed the slope with his hammer and chisel, and every evening he came home tired, with a little more stone and a little less hope.

                One day he passed the house of a rich merchant, and through the open door he saw silk cushions and golden lamps. "How I wish I were rich," he sighed. And a voice from the mountain answered, "So you shall be."

                At once Tarou was a merchant in a fine house. But soon he saw a prince ride by, with servants running at his side. "The prince is greater than I," he thought. "I wish I were a prince." And so he was.

                Then the summer came, and the sun beat down on the prince's golden carriage until he could hardly breathe. "The sun is mightier than any prince," he groaned. "I wish I were the sun." And so he was.

                He blazed across the sky, proud and bright, until a cloud drifted in front of him and hid the whole earth from his sight. "The cloud is stronger than the sun," he said. "I wish I were a cloud." And so he was.

                He poured rain on the valleys and flooded the rivers, but the mountain stood in the middle of it all and did not move. "The mountain is the strongest of all," he said. "I wish I were the mountain." And so he was.

                Now Tarou stood tall and still, and he felt very strong. Then, far below his feet, he felt a small tap, tap, tap. A stonecutter was chipping away at his side, and slowly a piece of the mountain came loose.

                "Even the mountain can be changed by a man with a hammer," thought Tarou. "I wish I were that man again."

                And the voice said gently, "So you are." Tarou woke on the mountain path with his hammer in his hand. The morning light was golden. And for the first time he did not wish for anything at all.
            """.trimIndent(),
        ),
        Story(
            id = "seed_farmers_horse",
            motif = Motif.HORSE,
            scene = app.murmur.audio.AmbienceKind.WIND,
            title = "Maybe: The Farmer and His Horse",
            author = "Chinese parable, retold",
            category = Categories.WISDOM,
            moods = setOf("patience", "calm", "learn"),
            summary = "Good luck, bad luck, who can say? An old farmer takes each day as it comes.",
            rights = Rights.PUBLIC_DOMAIN,
            body = """
                Once there was an old farmer who lived in a small village with his son and his one good horse. One morning the horse pushed open the gate and ran away into the hills.

                The neighbors gathered at his door. "What terrible luck," they said.

                The farmer shrugged. "Maybe," he said.

                A week later the horse came back, and behind it walked a dozen wild horses from the hills. "What wonderful luck!" said the neighbors.

                "Maybe," said the farmer.

                The next day his son tried to ride one of the wild horses. It threw him to the ground, and his leg was broken. "What terrible luck," said the neighbors.

                "Maybe," said the farmer, and he sat by his son's bed and brought him tea.

                Soon after, soldiers came marching through the village, calling every young man to the war. But they looked at the farmer's son, lying with his leg in a splint, and they passed him by.

                "What wonderful luck!" said the neighbors.

                The farmer looked out at the hills, where the evening was turning gold. "Maybe," he said softly. And he went inside to light the lamp.
            """.trimIndent(),
        ),
        Story(
            id = "seed_stone_soup",
            motif = Motif.POT,
            scene = app.murmur.audio.AmbienceKind.FIRE,
            title = "Stone Soup",
            author = "European folktale, retold",
            category = Categories.WISDOM,
            moods = setOf("inspire", "learn", "calm"),
            summary = "A hungry traveler, an empty pot, and a village that learns to share.",
            rights = Rights.PUBLIC_DOMAIN,
            body = """
                On a cold evening, a tired traveler came to a village at the edge of a forest. He knocked on one door after another, asking for a little food. But the people had little, and each one said, "I am sorry, I have nothing to spare."

                The traveler only smiled. He set down his pack, borrowed a great iron pot, and filled it with water from the well. Then he built a small fire in the square and placed a smooth round stone in the pot.

                Curious faces appeared at the windows. "What are you cooking?" asked a woman.

                "Stone soup," said the traveler. "It is nearly perfect. It only needs a little cabbage, if anyone could spare it."

                The woman went home and came back with a cabbage. Then a boy brought carrots. A baker brought salt and a handful of barley. An old man brought potatoes, and someone else an onion, and someone else a bone from the Sunday meal.

                The soup bubbled softly, and the smell drifted over the rooftops. One by one the villagers came out and sat around the fire, and bowls and spoons appeared from nowhere.

                They ate until they were warm and full, and they talked and laughed together as they had not in years.

                In the morning, the traveler lifted the stone from the pot, wiped it clean, and left it on the well for the next hungry stranger. "It is a very good stone," he said. "But it is the sharing that makes the soup."
            """.trimIndent(),
        ),
        Story(
            id = "seed_lantern_hill",
            motif = Motif.LANTERN,
            scene = app.murmur.audio.AmbienceKind.NIGHT,
            title = "The Lantern on the Hill",
            author = "Murmur",
            category = Categories.CALM,
            moods = setOf("sleep", "calm"),
            summary = "A slow, quiet walk to help the day settle.",
            rights = Rights.ORIGINAL,
            body = """
                The day is over now. Everything that needed to be done has been done, and everything else can wait until morning.

                Imagine a small path that begins at your door. The evening air is cool and soft. The sky is the deep blue that comes just after the sun has gone, and the first stars are quietly arriving.

                At the end of the path, there is a hill, not steep, just gentle. At the top of the hill hangs a lantern, glowing a warm gold. You walk toward it slowly. There is no hurry here.

                With every step, your shoulders loosen. With every breath, the air grows quieter. The grass beside the path sways, and somewhere far away a night bird sings once, and then is still.

                When you reach the top, you sit beside the lantern. Its light is not bright. It is only enough to feel safe. You can see the valley below, with its small windows glowing, one by one going dark as the world rests.

                You place everything you carried today beside the lantern: the worries, the plans, the unfinished things. They do not disappear, but they can rest here for the night, and you can pick them up again tomorrow, with new strength.

                The wind is gentle. The light is steady. Your breathing is slow and deep.

                The lantern will keep watch. You can rest now. Goodnight.
            """.trimIndent(),
        ),
    )
}
