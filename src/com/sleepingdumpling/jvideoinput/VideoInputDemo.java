/**
 * Copyright (c) 2013, The Sleeping Dumpling LLC and the individual contributors.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *   * 	Redistributions of source code must retain the above copyright notice,
 * 	this list of conditions and the following disclaimer.
 *
 *   *	Redistributions in binary form must reproduce the above copyright notice,
 * 	this list of conditions and the following disclaimer in the documentation
 * 	and/or other materials provided with the distribution.
 *
 *   *	Neither the name of the Sleeping Dumpling LLC nor the names of its
 * 	contributors may be used to endorse or promote products derived from this
 * 	software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.sleepingdumpling.jvideoinput;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.WritableRaster;
import java.text.DecimalFormat;
import java.util.concurrent.locks.LockSupport;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

public class VideoInputDemo extends JPanel {

	private BufferedImage displayImage;
	private double videoFps;
	private boolean mirrorImage = true;
	private DecimalFormat fpsFormat;
	private final static long ONE_SECOND_IN_NANOS = 1000000000L;
	
	public VideoInputDemo(int width, int height, final int fps) {
		this.fpsFormat = new DecimalFormat("#0.0");
		this.setPreferredSize(new Dimension(width, height));
		this.startRetrieverThread(width, height, fps);
	}
	
	private void startRetrieverThread(final int width, final int height, final int fps) {
		Thread videoFrameRetrieverThread = new Thread() {
			
			public void run() {
				retrieveAndDisplay(width, height, fps);
			}
		};
		videoFrameRetrieverThread.start();
		
	}
	
	private void retrieveAndDisplay(int width, int height, int fps) {
		try {
			final VideoInput videoInput = new VideoInput(width, height);
			final long interval = ONE_SECOND_IN_NANOS / fps;
			long lastReportTime = -1;
			long imgCnt = 0;
			
			while (true) {
				long start = System.nanoTime();
				try {
					VideoFrame vf = videoInput.getNextFrame(null);
					if (vf != null) {
						this.displayImage = getRenderingBufferedImage(vf);
						imgCnt++;
						
						long now = System.nanoTime();
						if (lastReportTime != -1 &&  now - lastReportTime >= ONE_SECOND_IN_NANOS) {
							videoFps = ((double)imgCnt*ONE_SECOND_IN_NANOS)/(now - lastReportTime);
							imgCnt = 0;
							lastReportTime = now;
						}
						else if (lastReportTime == -1) {
							lastReportTime = now;
						}
						
						SwingUtilities.invokeAndWait(new Runnable() {
							public void run() {
								paintImmediately(0, 0, getWidth(), getHeight());
							}
						});
					}
				}
				catch (Exception ex) {
					ex.printStackTrace();
				}
				finally {
					long end = System.nanoTime();
					long waitTime = interval - (end - start);
					if (waitTime > 0) {
						LockSupport.parkNanos(waitTime);
					}
				}
			}
		}
		catch (VideoInputException e1) {
			e1.printStackTrace();
		}
	}
	
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (displayImage != null) {
			Graphics2D g2 = (Graphics2D) g.create();
			if (this.mirrorImage) {
				//draw image, mirror it...
				AffineTransform xform = AffineTransform.getTranslateInstance(displayImage.getWidth(),0);  
				xform.scale(-1, 1); 
				g2.drawImage(displayImage, xform, null);
			}
			else {
				g2.drawImage(displayImage, 0, 0, null);
			}
			
			String fpsString = this.fpsFormat.format(this.videoFps) + " fps";
			int textWidth = (int) g2.getFontMetrics().getStringBounds(fpsString, g2).getWidth();
			g2.setColor(Color.BLACK);
			g2.drawString(fpsString, getWidth() - textWidth - 5, getHeight() - 5);
			g2.setColor(Color.ORANGE);
			g2.drawString(fpsString, getWidth() - textWidth - 6, getHeight() - 6);
			g2.dispose();
		}
	}
	
	public void setMirror(boolean mirror) {
		this.mirrorImage = mirror;
		this.repaint();
	}
	
	public boolean isMirror() {
		return this.mirrorImage;
	}
	
	public BufferedImage getRenderingBufferedImage(VideoFrame videoFrame) {
		GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
		BufferedImage img = gc.createCompatibleImage(videoFrame.getWidth(), videoFrame.getHeight(), Transparency.TRANSLUCENT);
		if (img.getType() == BufferedImage.TYPE_INT_ARGB 
				|| img.getType() == BufferedImage.TYPE_INT_ARGB_PRE
				|| img.getType() == BufferedImage.TYPE_INT_RGB) {
			WritableRaster raster = img.getRaster();
			DataBufferInt dataBuffer = (DataBufferInt) raster.getDataBuffer();
			
			byte[] data = videoFrame.getRawData();
			addAlphaChannel(data, data.length, dataBuffer.getData());
			return img; //convert the data ourselves, the performance is much better
		}
		else {
			return videoFrame.getBufferedImage(); //much slower when drawing it on the screen.
		}
	}	
	
	private void addAlphaChannel(byte[] rgbBytes, int bytesLen, int[] argbInts) {
		for (int i = 0, j = 0; i < bytesLen; i += 3, j++) {
			argbInts[j] = ((byte) 0xff) << 24 			|		// Alpha
					(rgbBytes[i] << 16) & (0xff0000) 	|		// Red
					(rgbBytes[i + 1] << 8) & (0xff00) 	|		// Green
					(rgbBytes[i + 2]) & (0xff); 				// Blue
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int width = 640;
		int height = 480;
		int fps = 30;
		if (args.length >= 2) {
			width = Integer.parseInt(args[0]);
			height = Integer.parseInt(args[1]);
		}
		
		if (args.length == 3) {
			fps = Integer.parseInt(args[2]);
		}
		
		final VideoInputDemo videoInputDemoPanel = new VideoInputDemo(width, height, fps);
		
		final JToggleButton btnMirror = new JToggleButton("Mirror");
		btnMirror.setSelected(videoInputDemoPanel.isMirror());
		
		btnMirror.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				videoInputDemoPanel.setMirror(btnMirror.isSelected());
			}
			
		});
		
		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		btnPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		btnPanel.add(btnMirror);
		
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(btnPanel, BorderLayout.NORTH);
		frame.getContentPane().add(videoInputDemoPanel, BorderLayout.CENTER);
		frame.setResizable(false);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
}