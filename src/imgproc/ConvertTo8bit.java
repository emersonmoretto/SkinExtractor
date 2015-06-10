package imgproc;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class ConvertTo8bit {

	public static BufferedImage convert(BufferedImage input){
		BufferedImage img = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
	    Graphics g = img.getGraphics();
	    g.drawImage(input, 0, 0, null);
	    g.dispose();
	    return img;		  
	}
}
