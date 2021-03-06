package dragonBones.objects;

import flash.geom.ColorTransform;
import flash.geom.Point;

/** @private */
final public class SlotFrame extends Frame
{
	//NaN:no tween, 10:auto tween, [-1, 0):ease in, 0:line easing, (0, 1]:ease out, (1, 2]:ease in out
	public double tweenEasing;
	public int displayIndex;
	public boolean visible;
	public double zOrder;
	public ColorTransform color;


	public SlotFrame()
	{
		super();

		tweenEasing = 10;
		displayIndex = 0;
		visible = true;
		zOrder = Double.NaN;
	}

	@Override public void dispose()
	{
		super.dispose();
		color = null;
	}

	public boolean getColorChanged()
	{
		if(color != null && (color.alphaMultiplier != 1 || color.alphaOffset != 0 ||
					color.blueMultiplier != 1 || color.blueOffset != 0 ||
					color.greenMultiplier != 1 || color.greenOffset != 0 ||
					color.redMultiplier != 1 || color.redOffset != 0))
		{
			return true;
		}
		return false;
	}
}

