package imgproc;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;


public class NiBlack {

	private static int width;
	private static int height;

	/** Vari?veis declarativas */
	
	/** Pixel do objeto */
	public final int pixel_ob = 0xffffffff; 

	/** Pixel de fundo*/
    public final int pixel_bg = 0xff000000; 

	/**
	 * Binarizaçao por NiBlack
	 */
	public static BufferedImage binarize(BufferedImage imgBuff) {

		width = imgBuff.getWidth();
		height = imgBuff.getHeight();
		
		BufferedImage img = deepCopy(imgBuff);

		// Tamanho da janela (para cada lado)
		int janela = 3;
		
		//Varrendo a imagem inteira
		for (int c = 0; c < width; c++) {
			for (int r = 0; r < height; r++) {
				
				int acc = 0;
				
				// Varrendo a janela pra pegar a media
				for(int ji = -janela ; ji < janela ; ji++){
					for(int jj = -janela ; jj < janela ; jj++){
						
						// acumulando os valores pra depois pegar a media - o if serve para proteger a janela de nao andar pra fora da imagem 
						if(c+ji >= 0 && c+ji < width)
							if(r+jj >= 0 && r+jj < height)
								acc += imgBuff.getRGB(c+ji, r+jj) & 0x00ff0000 >> 16;
					}
				}
				
				// eh maior que a media?
				int pixel = img.getRGB(c, r) & 0x00ff0000 >> 16;
				if(pixel > acc / ((janela*2) * (janela*2)) )
					img.setRGB(c, r, 0x00FFFFFF );
				else
					img.setRGB(c, r, 0x00000000 );
			}
		}
		
		return img;
	}

	static BufferedImage deepCopy(BufferedImage bi) {
		 ColorModel cm = bi.getColorModel();
		 boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		 WritableRaster raster = bi.copyData(null);
		 return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
		}
	
}
