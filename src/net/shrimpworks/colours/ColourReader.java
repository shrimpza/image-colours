package net.shrimpworks.colours;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class ColourReader {

	public static HSBColour averageColour(BufferedImage image, float resolution) {
		final List<Integer> samples = readImage(image, resolution);

		final int[] totalRGB = new int[] { 0, 0, 0 };

		samples.parallelStream().forEach(rgb -> {
			totalRGB[0] += (rgb >> 16) & 0xFF;
			totalRGB[1] += (rgb >> 8) & 0xFF;
			totalRGB[2] += (rgb) & 0xFF;
		});

		return new HSBColour(Color.RGBtoHSB(totalRGB[0] / samples.size(),
											totalRGB[1] / samples.size(),
											totalRGB[2] / samples.size(), null));
	}

	public static SortedSet<ColourVolume> colourVolumes(BufferedImage image, float resolution) {
		final List<Integer> samples = readImage(image, resolution);

		final List<Hue> hues = new ArrayList<>();
		hues.add(Hue.RED);
		hues.add(Hue.YELLOW);
		hues.add(Hue.GREEN);
		hues.add(Hue.CYAN);
		hues.add(Hue.BLUE);
		hues.add(Hue.MAGENTA);

		final Map<Color, List<HSBColour>> colorList = new HashMap<>();

		samples.parallelStream().forEach(rgb -> {
			HSBColour hsb = new HSBColour(Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, (rgb) & 0xFF, null));
			Color color = null;

			// TODO provide way of specifying threshold values for black/white/grey

			// handel black/white/grey separately
			if (hsb.saturation() == 0f && hsb.brightness() == 1f) {
				color = Color.WHITE;
			} else if (hsb.hue() == 0f && hsb.saturation() == 0f && hsb.brightness() > 0f) {
				color = Color.GRAY;
			} else if (hsb.brightness() == 0f) {
				color = Color.BLACK;
			} else {
				for (Hue hue : hues) if (hue.matches(hsb)) color = hue.colors()[0];
			}

			// guess this eliminates any advantage parallelStream() provides :| heh
			synchronized (colorList) {
				if (!colorList.containsKey(color)) colorList.put(color, new ArrayList<>());
				colorList.get(color).add(hsb);
			}
		});

		// TODO refactor to return list of averaged HSV colours, rather than solid colours

		return colorList.entrySet().stream()
						.map(e -> new ColourVolume(e.getKey(), ((float)e.getValue().size() / (float)samples.size())))
						.collect(Collectors.toCollection(TreeSet::new));
	}

	private static List<Integer> readImage(BufferedImage image, float resolution) {
		if (resolution < 0.0) throw new IllegalArgumentException("Resolution value may not be lower than 0.0");
		if (resolution > 1.0) throw new IllegalArgumentException("Resolution value may not exceed 1.0");

		final int xStep = image.getWidth() / (int)(image.getWidth() * resolution);
		final int yStep = image.getHeight() / (int)(image.getHeight() * resolution);

		final List<Integer> result = new ArrayList<>();
		for (int x = 0; x < image.getWidth(); x += xStep) {
			for (int y = 0; y < image.getHeight(); y += yStep) {
				result.add(image.getRGB(x, y));
			}
		}

		return result;
	}
}
