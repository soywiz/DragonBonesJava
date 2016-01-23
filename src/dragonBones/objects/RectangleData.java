package dragonBones.objects;

import flash.geom.Point;

public final class RectangleData implements IAreaData
{
	public String name;

	public double width;
	public double height;
	public DBTransform transform;
	public Point pivot;

	public RectangleData()
	{
		width = 0;
		height = 0;
		transform = new DBTransform();
		pivot = new Point();
	}

	public void dispose()
	{
		transform = null;
		pivot = null;
	}
}
