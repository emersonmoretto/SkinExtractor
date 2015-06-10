package imgproc;


/**
 * SkinExtractor by Emerson G Moretto (emoretto@lsi.usp.br)
 * Baseado na trasnforma??o proposta por Alain Lebret (University Pierre and Marie Curie - France).
 * V. 1.2
 *  
 *  Faz a transforma??o do espa?o RGB para YCbCr 
 *
 * * Y = 0.29900 * R + 0.58700 * G + 0.11400 * B Cb = -0.16874 * R - 0.33126 * G +
 * 0.50000 * B + 0x80 Cr = 0.50000 * R - 0.41869 * G - 0.08131 * B + 0x80
 * 
 * R = Y + 1.40200 * (Cr - 0x80) G = Y - 0.34414 * (Cb - 0x80) - 0.71414 * (Cr -
 * 0x80) B = Y + 1.77200 * (Cb - 0x80)
 * 
 *  
 * - TODO: 
 * Tentar aprimorar a escolha de candidado ? pele, est? errando um pouco ainda com tons de vermelho. 
 * Acredito ser tamb?m por causa da qualidade da minha webcam 
 * 
 * http://www.moretto.eng.br/emerson
 **/


import java.awt.image.BufferedImage;


public class SkinExtractor {

	private int width;
	private int height;

	/** Vari?veis declarativas */
	
	/** Pixel de fundo*/
	public final int pixel_bg = 0xffffffff; 
	/** Pixel do objeto */
    public final int pixel_ob = 0xff000000; 
	/** Cb min threshold  */
	public static double Cb_MIN_THRESHOLD = 95;
	/** Cb max threshold  */
	public static double Cb_MAX_THRESHOLD = 140;
	/** Cr min threshold  */
	public static double Cr_MIN_THRESHOLD = 140;
	/** Cr max threshold  */
	public static double Cr_MAX_THRESHOLD = 165;



	/**
	 * M?todo Extract
	 * 
	 * recebe uma imagem (BufferedImage) e o que ? pele vira pixel_ob, o que n?o ? vira pixel_bg
	 */
	public int[] extract(BufferedImage imgBuff) {

		width = imgBuff.getWidth();
		height = imgBuff.getHeight();

		int length = width * height;

		int[] img = new int[ length  ];
		int[] regions = new int[length];

		img = (int[]) imgBuff.getRGB(0, 0, width, height, img, 0, width);
		
		//Aplica a transformada para YCbCr
		this.RGB2YCbCr(img, regions);
		
		//Se ? pele vira pixel_ob caso contr?rio pixel_bg
		for (int r = 0; r < height; r++) {
			for (int c = 0; c < width; c++) {
				int pos = (r * width) + c;
				if(regions[pos] > 0){
					//imgBuff.setRGB(r, c, 0x00FFFFFF);
		            img[pos] = pixel_ob;
				}else{
					imgBuff.setRGB(c, r, 0x00000000);
					img[pos] = pixel_bg;
				}
				
			}
		}
		return img;
	}

	/**
	 * A transforma??o em si
	 * 
	 * @param img
	 *            a imagem
	 * @param mask
	 *            m?scara
	 */
	public void RGB2YCbCr(int[] img, int[] regions) {
		int red;
		int green;
		int blue;
		double Cb;
		double Cr;

		for (int r = 0; r < height; r++) {
			for (int c = 0; c < width; c++) {
				int pos = (r * width) + c;
				int p = img[pos];

				red = (p & 0x00FF0000) >> 16;
				green = (p & 0x0000FF00) >> 8;
				blue = (p & 0x000000FF);
				Cb = (-0.16874 * red) - (0.33126 * green) + (0.50000 * blue)
						+ 0x80;
				Cr = (0.50000 * red) - (0.41869 * green) - (0.08131 * blue)
						+ 0x80;
				regions[pos] = ((Cr > Cr_MIN_THRESHOLD)
						&& (Cr < Cr_MAX_THRESHOLD) && (Cb > Cb_MIN_THRESHOLD) && (Cb < Cb_MAX_THRESHOLD)) ? 255
						: 0;
			}
		}
	}

}
