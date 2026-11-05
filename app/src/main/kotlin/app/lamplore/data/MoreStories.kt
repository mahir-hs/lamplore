package app.lamplore.data

import app.lamplore.audio.AmbienceKind

/** More seed stories: public-domain fables and tales, retold, plus original sleep stories. */
object MoreStories {
    val all: List<Story> = listOf(
        Story(
            id = "seed_fox_grapes",
            plates = listOf(Plate("crane_fox_grapes", Books.CRANE, .6f, .02f, 1f, .98f)),
            motif = Motif.FOX_GRAPES,
            scene = AmbienceKind.FOREST,
            title = "The Fox and the Grapes",
            author = "Aesop, retold",
            category = Categories.FABLES,
            moods = setOf("learn", "calm"),
            summary = "A hungry fox, a high vine, and the stories we tell when we cannot reach something.",
            rights = Rights.PUBLIC_DOMAIN,
            body = """
                On a warm afternoon, a fox came padding through an old vineyard. He had walked all morning, and his stomach was as empty as a drum.

                High above him, on a vine that curled over a stone wall, hung a cluster of grapes. They were round and purple and shining in the sun. "Just what I need," said the fox, licking his lips.

                He crouched low and jumped. His paws scraped the wall, but the grapes swung far above him. He tried again. And again. He ran back to get a better start, and sprang with all his strength, but the grapes only trembled a little and hung still.

                The fox sat down, panting. A sparrow on the wall watched him with her head tilted to one side.

                "Giving up?" she asked.

                The fox lifted his chin. "Those grapes are probably sour anyway," he said. "I would not eat them if you paid me."

                And he walked away with his tail held high, as though he had never wanted them at all.

                The sparrow smiled, and the afternoon went on. It is easy to call something worthless when we cannot reach it. It is braver to say, "I wanted that, and I will try a different way tomorrow."
            """.trimIndent(),
        ),
        Story(
            id = "seed_boy_wolf",
            motif = Motif.WOLF,
            scene = AmbienceKind.NIGHT,
            title = "The Boy Who Cried Wolf",
            author = "Aesop, retold",
            category = Categories.FABLES,
            moods = setOf("learn", "patience"),
            summary = "A shepherd boy plays a trick, and learns what trust is worth.",
            rights = Rights.PUBLIC_DOMAIN,
            body = """
                A shepherd boy named Nico watched his father's sheep on a green hillside above the village. It was quiet work, and the days were long, and Nico was often bored.

                One afternoon he had an idea. He ran to the edge of the hill and shouted, "Wolf! Wolf! A wolf is among the sheep!"

                The villagers dropped their tools and ran up the hill. But when they arrived, panting, they found the sheep grazing peacefully and Nico rolling on the grass, laughing.

                "There is no wolf," he giggled. The villagers shook their heads and walked home without a word.

                A few days later, Nico was bored again. "Wolf! Wolf!" he cried. And again the villagers came running, and again they found nothing but a grinning boy.

                Then, one evening as the sun went down, a real wolf slipped out of the trees. Its eyes were yellow, and the sheep began to scatter.

                "Wolf!" shouted Nico, his voice shaking. "Please, there is a real wolf! Help!"

                In the village, people heard him and looked up for a moment. "He is playing his game again," they said, and they went on with their suppers.

                That night Nico lost many of his father's sheep. He sat by the empty fold, wet with tears, and he understood at last: a liar is not believed, even when he finally tells the truth.

                From that day on, Nico told only true things. It took a long time, but slowly, one honest word at a time, the village began to listen to him again.
            """.trimIndent(),
        ),
        Story(
            id = "seed_ant_grasshopper",
            motif = Motif.ANT,
            scene = AmbienceKind.STREAM,
            title = "The Ant and the Grasshopper",
            author = "Aesop, retold",
            category = Categories.FABLES,
            moods = setOf("learn", "patience"),
            summary = "One sings all summer, one works all summer, and winter arrives for both.",
            rights = Rights.PUBLIC_DOMAIN,
            body = """
                All through the golden summer, a grasshopper hopped about a sunny field, singing at the top of his voice. The days were warm, the flowers were open, and the grasshopper saw no reason to worry.

                Nearby, a line of ants marched back and forth along a narrow path, each one carrying a grain of wheat far bigger than her own head.

                "Why work so hard?" called the grasshopper. "Come and sing with me. There is plenty of food all around."

                "We are storing food for winter," said one ant, without stopping. "You should do the same."

                "Winter is far away," laughed the grasshopper, and he went on singing.

                But the days grew shorter, and the leaves turned red and fell. One morning the field was white with frost, and the flowers were gone. The grasshopper was cold, and hungry, and very small.

                He hopped to the ants' door and knocked softly. "Please," he said, "I have nothing to eat."

                The ant looked at him for a long moment. Then she stepped aside. "Come in out of the cold," she said. "There is a little to share. But in spring, my friend, you will help us carry."

                The grasshopper bowed his head. "I will," he said. "And I will sing for you, too, in the evenings, if you like."

                So it was that the ants and the grasshopper spent the winter together. And when spring came, the grasshopper carried grain beside them, singing as he went.
            """.trimIndent(),
        ),
        Story(
            id = "seed_golden_goose",
            motif = Motif.GOOSE,
            scene = AmbienceKind.CALM,
            title = "The Goose That Laid the Golden Eggs",
            author = "Aesop, retold",
            category = Categories.FABLES,
            moods = setOf("learn", "patience"),
            summary = "A farmer who could not wait for tomorrow's gold.",
            rights = Rights.PUBLIC_DOMAIN,
            body = """
                A poor farmer and his wife kept a single goose. One morning, the farmer went to the nest and found something shining among the straw. It was an egg, and it was made of pure gold.

                He carried it to the market, and it sold for more money than he had ever held. The next morning there was another golden egg. And the next morning, another.

                Soon the farmer and his wife had a fine house, warm blankets, and enough to eat every day. But the more they had, the more they wanted.

                "Why should we wait for one egg a day?" said the farmer one evening. "The goose must be full of gold inside. If we open her up, we shall have it all at once."

                "Perhaps we should be patient," said his wife softly.

                But the farmer would not listen. In the morning he took a knife to the nest, and he cut the goose open.

                Inside, there was nothing at all. She was only a goose, like any other. And now there was no goose, and no more golden eggs.

                The farmer sat by the empty nest for a long time. He had been given a gift that came slowly, one day at a time, and he had thrown it away because slow was not fast enough.

                Sometimes the best thing we have is the one that asks us to wait.
            """.trimIndent(),
        ),
        Story(
            id = "seed_ugly_duckling",
            motif = Motif.SWAN,
            scene = AmbienceKind.STREAM,
            title = "The Ugly Duckling",
            author = "Hans Christian Andersen, retold",
            category = Categories.FABLES,
            moods = setOf("inspire", "calm"),
            summary = "A lonely bird finds out what he was always meant to be.",
            rights = Rights.PUBLIC_DOMAIN,
            body = """
                It was summer in the countryside, and by a quiet pond a mother duck sat on her nest. One by one the eggs cracked open, and out came small yellow ducklings, peeping and shaking out their feathers. But the biggest egg was still whole.

                At last it cracked, and out tumbled a large, gray, awkward bird. "He is not like the others," whispered the ducks of the farmyard. "He is so ugly."

                The little gray bird tried to swim, and he swam beautifully, better than any of them. But the other ducks pecked at him, and the hens ruffled their feathers, and even the farm girl shooed him away. Sadly, he ran off across the fields.

                He lived alone among the reeds through the autumn. When the wild geese flew over he longed to follow them, and when winter came he nearly froze in the ice. A kind farmer carried him home, but the noisy children frightened him and he fled again into the snow.

                It was a long, cold winter. But it ended. One morning the sun grew warm, the larks began to sing, and he found that his wings had grown strong. With a great beat he rose into the air.

                Soon he came to a garden with a still, clear pond. Three beautiful white birds glided across the water. "I will go to them," he thought. "They may kill me, for I am so ugly. But I would rather be near them than be alone."

                He bowed his head and looked down at the water. And there, in the mirror of the pond, he saw a swan.

                The other swans swam to him and stroked his neck with their beaks. Children threw bread and cried, "The new one is the most beautiful of all!"

                He ruffled his feathers and lifted his slender neck. He had never dreamed of so much happiness when he was the ugly duckling.
            """.trimIndent(),
        ),
        Story(
            id = "seed_blind_elephant",
            motif = Motif.ELEPHANT,
            scene = AmbienceKind.DESERT,
            title = "The Blind Men and the Elephant",
            author = "Indian parable, retold",
            category = Categories.WISDOM,
            moods = setOf("learn", "inspire"),
            summary = "Six wise men touch one elephant and each is sure of what it is.",
            rights = Rights.PUBLIC_DOMAIN,
            body = """
                Long ago in a village in India, there lived six blind men who were friends. One day they heard that an elephant had come to town, and though they had never seen one, they were eager to know what it was like. So they went together to meet it.

                The first man touched the elephant's side, broad and warm and solid. "An elephant is like a wall," he said.

                The second man felt the tusk, smooth and sharp. "No, it is like a spear," he said.

                The third man took the trunk in his hands as it curled and twisted. "You are both wrong," he said. "It is like a snake."

                The fourth man reached out and found the leg, thick and round. "It is like a tree," he declared.

                The fifth man touched the ear as it fanned slowly back and forth. "It is like a great fan," he said.

                The sixth man caught hold of the tail, thin and tufted. "An elephant is like a rope," he said.

                They began to argue. Each was sure he was right, and each grew louder, until their voices carried through the whole village.

                An old woman passing by stopped and listened. Then she laughed gently. "You are all right," she said, "and all of you are wrong. Each of you has touched one part. The elephant is all of these things together."

                The six men fell silent. Then, one by one, they reached out again, this time listening to each other as they went.

                When we hold only one piece of the truth, it is wise to ask what the others have found.
            """.trimIndent(),
        ),
        Story(
            id = "seed_lighthouse_keeper",
            motif = Motif.LIGHTHOUSE,
            scene = AmbienceKind.OCEAN,
            title = "The Lighthouse Keeper's Night",
            author = "Lamplore",
            category = Categories.CALM,
            moods = setOf("sleep", "calm"),
            summary = "A slow evening on a rock at the edge of the sea.",
            rights = Rights.ORIGINAL,
            body = """
                The sun has just gone down, and the sea has turned the color of slate. You are the keeper of a small white lighthouse on a rock at the edge of the world, and tonight there is nothing to do but keep the light burning.

                You climb the spiral stairs slowly, one hand on the cool rail. There are a hundred and twelve steps, and you count them without hurry. At the top there is a round room full of glass and quiet.

                You light the great lamp. It wakes gently, glowing amber, and the lens begins its slow turn. A long soft beam slides out across the water, and then slides back again.

                Far below, the waves roll in against the rock. They arrive, and they leave, and they arrive again. There is no end to the rhythm, and nothing about it that needs you to do anything at all.

                You pour a cup of tea and sit by the window. The steam rises in a thin line. Somewhere out on the dark water a ship's bell rings once, very far away, and the beam passes over it as if to say, you are not alone.

                The wind is soft tonight. It hums quietly around the tower, and the old walls hold you the way they have held every keeper before you.

                Your eyes grow heavy. The light turns. The sea breathes in, and out. In and out.

                The lamp will keep turning all night, and the ships will find their way. There is nothing left for you to guard. You can rest now.
            """.trimIndent(),
        ),
        Story(
            id = "seed_night_train",
            motif = Motif.TRAIN,
            scene = AmbienceKind.NIGHT,
            title = "The Night Train",
            author = "Lamplore",
            category = Categories.CALM,
            moods = setOf("sleep", "calm"),
            summary = "A slow sleeper train rolling through a sleeping country.",
            rights = Rights.ORIGINAL,
            body = """
                The station is almost empty. A single lamp glows yellow on the platform, and a small train waits beside it, breathing softly.

                You step aboard and find your compartment. There is a narrow bed with a heavy blanket, and a window that looks out on the dark. The seat smells faintly of old wood and clean linen.

                A whistle sounds, long and low. The carriage gives a gentle shudder, and then, so smoothly you hardly notice, the platform begins to slide away.

                The train finds its rhythm. Clack-clack, clack-clack. Clack-clack, clack-clack. It is the sound of something that knows exactly where it is going.

                Outside, the night rolls past. A farmhouse with one lit window. A field silver under the moon. A river, wide and slow, with the stars floating on it. A village asleep, every roof quiet.

                You lie back and pull up the blanket. The whole carriage rocks you like a cradle, left a little, right a little.

                You do not have to watch the road. You do not have to decide anything. Someone else is driving, and the tracks lead somewhere kind.

                The window turns from black to deep blue, and the stars blur and soften. Clack-clack. Clack-clack.

                Somewhere in the middle of the night, you stop counting the sounds, and the train carries you on.
            """.trimIndent(),
        ),
        Story(
            id = "seed_rain_cabin",
            motif = Motif.CABIN,
            scene = AmbienceKind.RAIN,
            title = "Rain on the Cabin Roof",
            author = "Lamplore",
            category = Categories.CALM,
            moods = setOf("sleep", "calm"),
            summary = "A small warm cabin, a long soft rain, and nowhere to be.",
            rights = Rights.ORIGINAL,
            body = """
                You have reached the cabin just in time. The first drops are falling as you close the door behind you, and the sound of the world goes soft.

                Inside, the room is small and warm. A little stove glows orange in the corner. There is a thick wool blanket on the bed, a lamp with a yellow shade, and a kettle already ticking as it heats.

                You take off your wet boots and set them by the stove. Your socks are damp, and you warm your toes in the glow.

                Now the rain begins in earnest. It drums on the roof, a thousand soft fingers, each tap a little different from the last. It runs down the windowpane in slow silver lines.

                You sit by the window with a cup of tea held in both hands. The steam curls up, and the glass fogs gently. Outside, the pine trees stand dark and patient, and the path has turned to a ribbon of shining mud.

                There is nothing that needs doing. The rain will fall whether you watch it or not. It will water the hills, and fill the stream, and wash the dust from every leaf.

                You climb under the blanket. The sound of the rain wraps around you, steady and kind. Your breath slows to match it. Tap, tap, tap. In. And out.

                The stove ticks as it cools. The rain goes on. You are warm, and dry, and nowhere else.
            """.trimIndent(),
        ),
        Story(
            id = "seed_still_lake",
            motif = Motif.BOAT,
            scene = AmbienceKind.STREAM,
            title = "A Boat on the Still Lake",
            author = "Lamplore",
            category = Categories.CALM,
            moods = setOf("sleep", "calm"),
            summary = "Drifting on a mountain lake as the light goes out of the sky.",
            rights = Rights.ORIGINAL,
            body = """
                The lake is perfectly still. It lies between the mountains like a dark mirror, holding the last pink light of the evening.

                You step into a small wooden boat, and it rocks once and settles. You push away from the dock with one gentle push of the oar, and then you let the oar rest across your knees.

                The boat drifts. It moves so slowly that at first you cannot tell whether it moves at all. Ripples spread out from the hull in wide, quiet rings, and fade, and are gone.

                Far above, the first star appears. Then another. They shine in the sky, and again in the water, so that the boat seems to float in the middle of a sphere of stars.

                A loon calls once from the far shore, a long and lonely sound, and then the lake takes the silence back.

                You lean back and look straight up. The mountains are black shapes against a sky of deep blue. The air is cool and clean and smells of pine.

                Water laps against the wood, soft as a whisper. Lap. And lap. And lap.

                You are not going anywhere, and you do not need to. The boat knows how to drift, and the lake knows how to hold it.

                Your eyelids grow heavy, and the stars blur, and the water keeps rocking you gently, gently, into sleep.
            """.trimIndent(),
        ),
        Story(
            id = "seed_moon_lullaby",
            motif = Motif.MOON,
            scene = AmbienceKind.CALM,
            title = "The Moon's Lullaby",
            author = "Lamplore",
            category = Categories.CALM,
            moods = setOf("sleep", "calm"),
            summary = "The moon visits every window, one by one, to say goodnight.",
            rights = Rights.ORIGINAL,
            body = """
                Every night, when the sky has turned the color of deep ink, the moon rises over the hills to say goodnight to the world.

                She is round and pale and kind. She does not hurry. She climbs the sky one slow step at a time, and she silvers everything she touches.

                First she visits the sleeping village. She lays a soft light on the rooftops, one after another, and the chimneys glow like candles. A cat on a wall looks up and blinks, and then curls her tail around her feet.

                Then she visits the river. It carries her reflection downstream like a floating lantern, and the reeds on the bank bend down to touch it.

                She visits the forest, and the owls turn their heads to greet her. She slips between the branches and paints silver stripes on the moss, and every sleeping animal feels her light and sleeps a little deeper.

                At last she visits a window, the window you are lying beside. She looks in, and her light falls across your blanket, gentle as a hand.

                "Goodnight," she whispers. "The day is finished. Everything you did was enough. Everything you did not do can wait."

                The stars come out around her, one at a time, to hum the old, slow song they have always known. You do not need to hear every note.

                The moon will stay until morning. She will watch over the village, and the river, and the forest, and you. You can close your eyes now. Goodnight.
            """.trimIndent(),
        ),
    )
}
