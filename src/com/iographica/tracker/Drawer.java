package com.iographica.tracker;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import com.iographica.core.Data;

class Drawer {
	private static final float RADIUS_TRESHOLD = 20;
	private static final int DELAY_DISTANCE_QUAD = 400;
	private float _scale;
	private Rectangle _rect;
	private float _radius;
	private FPoint _prevP;
	private FPoint _newP;
	private FPoint _stopP;
	private BufferedImage _image;
	private Graphics2D _graphics2D;
	private BufferedImage _imagePreview;
	private Graphics2D _previewGraphics2D;

	public Drawer() {
	}
	
	public void setupImages(Rectangle r, float s) {
		RenderingHints renderHints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		renderHints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		_rect = r;
		_scale = s;
		if (_image != null) {
			if (_image.getWidth() != r.width || _image.getHeight() != r.height) {
				_image.flush();
				_image = null;
				System.gc();
				_image = new BufferedImage(r.width, r.height, BufferedImage.TYPE_INT_ARGB);
			}
		} else {
			_image = new BufferedImage(r.width, r.height, BufferedImage.TYPE_INT_ARGB);
		}
		_graphics2D = (Graphics2D) _image.getGraphics();
		_graphics2D.setRenderingHints(renderHints);
		_graphics2D.setColor(new Color(0x000000));
		_graphics2D.setStroke(new BasicStroke(Data.STROKE_WEIGHT, BasicStroke.CAP_BUTT, BasicStroke.CAP_BUTT, 10));
		if (_imagePreview != null) {
			if (_imagePreview.getWidth() != r.width || _imagePreview.getHeight() != r.height) {
				_imagePreview.flush();
				_imagePreview = null;
				System.gc();
				_imagePreview = new BufferedImage(r.width, r.height, BufferedImage.TYPE_INT_ARGB);
			}
		} else {
			_imagePreview = new BufferedImage(r.width, r.height, BufferedImage.TYPE_INT_ARGB);
		}
		_previewGraphics2D = (Graphics2D) _imagePreview.getGraphics();
		_previewGraphics2D.setRenderingHints(renderHints);
		_previewGraphics2D.setColor(new Color(0x000000));
		_previewGraphics2D.setStroke(new BasicStroke(Data.STROKE_WEIGHT*s, BasicStroke.CAP_BUTT, BasicStroke.CAP_BUTT, 10));
		resetImages();
	}
	
	private void setDarkness(Graphics2D g) {
		g.setBackground(new Color(0, 0, 0, 0));
		if (Data.prefs.getBoolean(Data.USE_COLOR_SCHEME, false)) {
			g.setBackground(new Color(0, 0, 0, 210));
		}
	}

	public void prepareForUpdate() {
		_newP = new FPoint(0, 0);
		_prevP = new FPoint(0, 0);
		_stopP = new FPoint(0, 0);
		Point p = MouseInfo.getPointerInfo().getLocation();
		_newP.x = p.x;
		_newP.y = p.y;
		_newP.x -= _rect.x;
		_newP.y -= _rect.y;
		_prevP.be(_newP);
		_stopP.be(_newP);
		_radius = 0;
	}
	
	public void update() {
		Point p = MouseInfo.getPointerInfo().getLocation();
		_newP.x = p.x;
		_newP.y = p.y;
		_newP.x -= _rect.x;
		_newP.y -= _rect.y;
		boolean noMovement = _prevP.x == _newP.x && _prevP.y == _newP.y;
		if (!noMovement) {
			drawLine(_previewGraphics2D, _scale);
			drawLine(_graphics2D, 1.f);
		}
		if (Data.prefs.getBoolean(Data.IGNORE_MOUSE_STOPS, false)) {
			_prevP.be(_newP);
			return;
		}
		float dx = _newP.x - _stopP.x;
		float dy = _newP.y - _stopP.y;
		float d = dx * dx + dy * dy;
		if (d < DELAY_DISTANCE_QUAD) {
			_radius += .3;
			_prevP.be(_newP);
			return;
		}
		if (_radius > RADIUS_TRESHOLD) {
			_radius = Math.min(_radius, (float) Math.pow(_image.getHeight() * .25, 2));
			drawEllipse(_previewGraphics2D, _scale);
			drawEllipse(_graphics2D, 1.f);
		}
		_prevP.be(_newP);
		_stopP.be(_newP);
		_radius = 0;
	}
	
	void drawLine(Graphics2D g, float s) {
		g.setColor(getColor());
		g.drawLine((int)(_newP.x * s), (int)(_newP.y * s), (int)(_prevP.x * s), (int)(_prevP.y * s));
	}

	void drawEllipse(Graphics g, float s) {
		float haloDiameter = 2 * _radius * s;
		float dotDiameter = 2 * ((float) Math.sqrt(_radius)) * s;
		float n = 200f * Math.max(0f, 1f - 2f*(float) Math.sqrt(_radius)/RADIUS_TRESHOLD);
		int chanelColor = Data.prefs.getBoolean(Data.USE_COLOR_SCHEME, false) ? 0 : 255;
		Color c = new Color(chanelColor, chanelColor, chanelColor, (int)n);
		g.setColor(c);
		g.fillOval((int) (_prevP.x * s - haloDiameter * .5f), (int) (_prevP.y * s - haloDiameter * .5f), (int) haloDiameter, (int) haloDiameter);
		g.setColor(getColor());
		g.drawOval((int) (_prevP.x * s - haloDiameter * .5f), (int) (_prevP.y * s - haloDiameter * .5f), (int) haloDiameter, (int) haloDiameter);
		g.fillOval((int) (_prevP.x * s - dotDiameter * .5f), (int) (_prevP.y * s - dotDiameter * .5f), (int) dotDiameter, (int) dotDiameter);
	}
	
	private Color getColor() {
		Color c;
		if (Data.prefs.getBoolean(Data.USE_COLOR_SCHEME, false)) {
			//float n = (float)(new Date().getTime()%(long)600000);
			//n = n/60000.f/10.f;
			//n = (float)((Math.PI + Math.atan2(_newP.y - _prevP.y, _newP.x - _prevP.x))/2/Math.PI);
			float n = (float)(1+Math.atan2(_newP.y - _prevP.y, _newP.x - _prevP.x)/Math.PI);
			n = (n+.25f)%1;
			Color[] tc = {new Color(255,255,0), new Color(0,255,255), new Color(255,0,255)};
			Color c0 = tc[(int)(tc.length*n)];
			Color c1 = tc[(int)((tc.length*n+1)%tc.length)];
			n = (tc.length*n-(int)(tc.length*n));
			int cr = (int)(c0.getRed()+(c1.getRed() - c0.getRed())*n);
			int cg = (int)(c0.getGreen()+(c1.getGreen() - c0.getGreen())*n);
			int cb = (int)(c0.getBlue()+(c1.getBlue() - c0.getBlue())*n);
			c = new Color(cr, cg, cb);
		} else {
			c = new Color(0);
		}
		return c;
	}

	public Image getPreview() {
		return _imagePreview;
	}
	
	public BufferedImage getImage() {
		return _image;
	}
	
	public void resetImages() {
		setDarkness(_graphics2D);
		setDarkness(_previewGraphics2D);
		_graphics2D.clearRect(0, 0, _image.getWidth(), _image.getHeight());
		_previewGraphics2D.clearRect(0, 0, _imagePreview.getWidth(), _imagePreview.getHeight());
	}
}

class FPoint {
	public float x;
	public float y;

	public FPoint(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public void be(FPoint p) {
		x = p.x;
		y = p.y;
	}
}