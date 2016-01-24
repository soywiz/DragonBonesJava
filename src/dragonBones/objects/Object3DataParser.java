package dragonBones.objects;

import dragonBones.core.dragonBones_internal;
import dragonBones.utils.ConstValues;
import dragonBones.utils.DBDataUtil;
import flash.errors.ArgumentError;
import flash.geom.ColorTransform;
import flash.geom.Point;
import flash.utils.Dictionary;
/**
 * ...
 * @author sukui
 */
final public class Object3DataParser
{
	private static DragonBonesData tempDragonBonesData;

	//use namespace dragonBones_internal;

	public Object3DataParser()
	{

	}

	public static DragonBonesData parseSkeletonData(Object rawData, boolean ifSkipAnimationData=false, Dictionary outputAnimationDictionary = null)
	{
		if(rawData == null)
		{
			throw new ArgumentError();
		}

		String version = rawData[ConstValues.A_VERSION];
		switch (version)
		{
			case "2.3":
			case "3.0":
				break;

			default:
				throw new Error("Nonsupport version!");
		}

		int frameRate = int(rawData[ConstValues.A_FRAME_RATE]);

		DragonBonesData data = new DragonBonesData();
		data.name = rawData[ConstValues.A_NAME];
		tempDragonBonesData = data;
		var isGlobalData:Boolean = rawData[ConstValues.A_IS_GLOBAL] == "0" ? false : true;

		for (Object armatureObject : rawData[ConstValues.ARMATURE])
		{
			data.addArmatureData(parseArmatureData(armatureObject, data, frameRate, isGlobalData, ifSkipAnimationData, outputAnimationDictionary));
		}

		return data;
	}

	private static ArmatureData parseArmatureData(Object armatureObject, DragonBonesData data, int frameRate, boolean isGlobalData, boolean ifSkipAnimationData, Dictionary outputAnimationDictionary)
	{
		ArmatureData armatureData = new ArmatureData();
		armatureData.name = armatureObject[ConstValues.A_NAME];

		for (Object boneObject : armatureObject[ConstValues.BONE])
		{
			armatureData.addBoneData(parseBoneData(boneObject, isGlobalData));
		}

		for ( Object skinObj : armatureObject[ConstValues.SKIN])
		{
			for (Object slotObj in skinObj[ConstValues.SLOT])
			{
				armatureData.addSlotData(parseSlotData(slotObj));
			}
		}

		for (Object skinObject : armatureObject[ConstValues.SKIN])
		{
			armatureData.addSkinData(parseSkinData(skinObject, data));
		}

		armatureData.sortBoneDataList();

		if(isGlobalData)
		{
			DBDataUtil.transformArmatureData(armatureData);
		}

		Object animationObject;
		if(ifSkipAnimationData)
		{
			if(outputAnimationDictionary!= null)
			{
				outputAnimationDictionary[armatureData.name] = new Dictionary();
			}

			int index = 0;
			for (animationObject : armatureObject[ConstValues.ANIMATION])
			{
				if(index == 0)
				{
					armatureData.addAnimationData(parseAnimationData(animationObject, armatureData, frameRate, isGlobalData));
				}
				else if(outputAnimationDictionary != null)
				{
					outputAnimationDictionary[armatureData.name][animationObject[ConstValues.A_NAME]] = animationObject;
				}
				index++;
			}
		}
		else
		{
			for (animationObject in armatureObject[ConstValues.ANIMATION])
			{
				armatureData.addAnimationData(parseAnimationData(animationObject, armatureData, frameRate, isGlobalData));
			}
		}

		//for each(var rectangleObject:Object in armatureObject[ConstValues.RECTANGLE])
		//{
			//armatureData.addAreaData(parseRectangleData(rectangleObject));
		//}
		//
		//for each(var ellipseObject:Object in armatureObject[ConstValues.ELLIPSE])
		//{
			//armatureData.addAreaData(parseEllipseData(ellipseObject));
		//}

		return armatureData;
	}

	private static BoneData parseBoneData(Object boneObject, boolean isGlobalData)
	{
		BoneData boneData = new BoneData();
		boneData.name = boneObject[ConstValues.A_NAME];
		boneData.parent = boneObject[ConstValues.A_PARENT];
		boneData.length = Number(boneObject[ConstValues.A_LENGTH]);
		boneData.inheritRotation = getBoolean(boneObject, ConstValues.A_INHERIT_ROTATION, true);
		boneData.inheritScale = getBoolean(boneObject, ConstValues.A_INHERIT_SCALE, true);

		parseTransform(boneObject[ConstValues.TRANSFORM], boneData.transform);
		if(isGlobalData)//绝对数据
		{
			boneData.global.copy(boneData.transform);
		}
		//for each(var rectangleObject:Object in boneObject[ConstValues.RECTANGLE])
		//{
			//boneObject.addAreaData(parseRectangleData(rectangleObject));
		//}
		//
		//for each(var ellipseObject:Object in boneObject[ConstValues.ELLIPSE])
		//{
			//boneObject.addAreaData(parseEllipseData(ellipseObject));
		//}

		return boneData;
	}

	private static RectangleData parseRectangleData(Object rectangleObject)
	{
		RectangleData rectangleData = new RectangleData();
		rectangleData.name = rectangleObject[ConstValues.A_NAME];
		rectangleData.width = Number(rectangleObject[ConstValues.A_WIDTH]);
		rectangleData.height = Number(rectangleObject[ConstValues.A_HEIGHT]);

		parseTransform(rectangleObject[ConstValues.TRANSFORM], rectangleData.transform, rectangleData.pivot);

		return rectangleData;
	}

	private static EllipseData parseEllipseData(Object ellipseObject)
	{
		EllipseData ellipseData = new EllipseData();
		ellipseData.name = ellipseObject[ConstValues.A_NAME];
		ellipseData.width = Number(ellipseObject[ConstValues.A_WIDTH]);
		ellipseData.height = Number(ellipseObject[ConstValues.A_HEIGHT]);

		parseTransform(ellipseObject[ConstValues.TRANSFORM], ellipseData.transform, ellipseData.pivot);

		return ellipseData;
	}

	private static SlotData parseSlotData(Object slotObject)
	{
		SlotData slotData = new SlotData();
		slotData.name = slotObject[ConstValues.A_NAME];
		slotData.parent = slotObject[ConstValues.A_PARENT];
		slotData.zOrder = getNumber(slotObject,ConstValues.A_Z_ORDER,0)||0;
		slotData.blendMode = slotObject[ConstValues.A_BLENDMODE];
		slotData.displayIndex = 0;

		return slotData;
	}

	private static SkinData parseSkinData(Object skinObject, DragonBonesData data)
	{
		SkinData skinData = new SkinData();
		skinData.name = skinObject[ConstValues.A_NAME];

		for (Object slotObject : skinObject[ConstValues.SLOT])
		{
			skinData.addSlotData(parseSkinSlotData(slotObject, data));
		}

		return skinData;
	}

	private static SlotData parseSkinSlotData(Object slotObject, DragonBonesData data)
	{
		SlotData slotData = new SlotData();
		slotData.name = slotObject[ConstValues.A_NAME];
		slotData.parent = slotObject[ConstValues.A_PARENT];
		slotData.zOrder = getNumber(slotObject, ConstValues.A_Z_ORDER, 0) || 0;
		slotData.blendMode = slotObject[ConstValues.A_BLENDMODE];

		for (Object displayObject : slotObject[ConstValues.DISPLAY])
		{
			slotData.addDisplayData(parseDisplayData(displayObject, data));
		}

		return slotData;
	}

	private static DisplayData parseDisplayData(Object displayObject, DragonBonesData data)
	{
		DisplayData displayData = new DisplayData();
		displayData.name = displayObject[ConstValues.A_NAME];
		displayData.type = displayObject[ConstValues.A_TYPE];

		//displayData.pivot = data.addSubTexturePivot(
			//0,
			//0,
			//displayData.name
		//);

		parseTransform(displayObject[ConstValues.TRANSFORM], displayData.transform, displayData.pivot);

		if (tempDragonBonesData)
		{
			tempDragonBonesData.addDisplayData(displayData);
		}
		return displayData;
	}

	/** @private */
	private static AnimationData parseAnimationData(animationObject:Object, armatureData:ArmatureData, frameRate:uint, isGlobalData:Boolean)
	{
		var animationData:AnimationData = new AnimationData();
		animationData.name = animationObject[ConstValues.A_NAME];
		animationData.frameRate = frameRate;
		animationData.duration = Math.round((Number(animationObject[ConstValues.A_DURATION]) || 1) * 1000 / frameRate);
		animationData.playTimes = int(getNumber(animationObject, ConstValues.A_LOOP, 1));
		animationData.fadeTime = getNumber(animationObject, ConstValues.A_FADE_IN_TIME, 0) || 0;
		animationData.scale = getNumber(animationObject, ConstValues.A_SCALE, 1) || 0;
		//use frame tweenEase, NaN
		//overwrite frame tweenEase, [-1, 0):ease in, 0:line easing, (0, 1]:ease out, (1, 2]:ease in out
		animationData.tweenEasing = getNumber(animationObject, ConstValues.A_TWEEN_EASING, NaN);
		animationData.autoTween = getBoolean(animationObject, ConstValues.A_AUTO_TWEEN, true);

		for each(var frameObject:Object in animationObject[ConstValues.FRAME])
		{
			var frame:Frame = parseTransformFrame(frameObject, null, frameRate, isGlobalData);
			animationData.addFrame(frame);
		}

		parseTimeline(animationObject, animationData);

		var displayIndexChangeSlotTimelines:Vector.<SlotTimeline> = new Vector.<SlotTimeline>();
		var displayIndexChangeTimelines:Vector.<TransformTimeline> = new Vector.<TransformTimeline>();
		var lastFrameDuration:int = animationData.duration;
		var displayIndexChange:Boolean;

		for each(var timelineObject:Object in animationObject[ConstValues.TIMELINE])
		{
			var timeline:TransformTimeline = parseTransformTimeline(timelineObject, animationData.duration, frameRate, isGlobalData);
			lastFrameDuration = Math.min(lastFrameDuration, timeline.frameList[timeline.frameList.length - 1].duration);
			animationData.addTimeline(timeline);

			var slotTimeline:SlotTimeline = parseSlotTimeline(timelineObject, animationData.duration, frameRate, isGlobalData);
			if (slotTimeline.frameList.length > 0)
			{
				lastFrameDuration = Math.min(lastFrameDuration, slotTimeline.frameList[slotTimeline.frameList.length - 1].duration);
				animationData.addSlotTimeline(slotTimeline);
				if (animationData.autoTween && !displayIndexChange)
				{

					var slotFrame:SlotFrame;
					for (var i:int = 0, len:int = slotTimeline.frameList.length; i < len; i++)
					{
						slotFrame = slotTimeline.frameList[i] as SlotFrame;
						if (slotFrame && slotFrame.displayIndex < 0)
						{
							displayIndexChange = true;
							break;
						}
					}
				}
			}
		}
		/**
		 * 如果有slot的displayIndex为空的情况，那么当autoTween为ture时，它对应的bone的补间应该去掉
		 * 以下就是处理这种情况，把autoTween的全局的tween应用到每一帧上，然后把autoTween变为false
		 * 此时autoTween就不起任何作用了
		 */
		var animationTween:Number = animationData.tweenEasing;
		if (displayIndexChange)
		{
			len = animationData.slotTimelineList.length;
			for (i = 0; i < len; i++)
			{
				slotTimeline = animationData.slotTimelineList[i];
				timeline = animationData.timelineList[i];
				var curFrame:TransformFrame;
				var curSlotFrame:SlotFrame;
				var nextSlotFrame:SlotFrame;
				for (var j:int = 0, jlen:int = slotTimeline.frameList.length; j < jlen; j++)
				{
					curSlotFrame = slotTimeline.frameList[j] as SlotFrame;
					curFrame = timeline.frameList[j] as TransformFrame;
					nextSlotFrame = (j == jlen - 1) ? slotTimeline.frameList[0] as SlotFrame : slotTimeline.frameList[j + 1] as SlotFrame;
					if (curSlotFrame.displayIndex < 0 || nextSlotFrame.displayIndex < 0)
					{
						curFrame.tweenEasing = curSlotFrame.tweenEasing = NaN;
					}
					else if (animationTween == 10)
					{
						curFrame.tweenEasing = curSlotFrame.tweenEasing = 0;
					}
					else if (!isNaN(animationTween))
					{
						curFrame.tweenEasing = curSlotFrame.tweenEasing = animationTween;
					}
					else if (curFrame.tweenEasing == 10)
					{
						curFrame.tweenEasing = 0;
					}
				}
			}
			animationData.autoTween = false;
		}
		if(animationData.frameList.length > 0)
		{
			lastFrameDuration = Math.min(lastFrameDuration, animationData.frameList[animationData.frameList.length - 1].duration);
		}
		animationData.lastFrameDuration = lastFrameDuration;

		DBDataUtil.addHideTimeline(animationData, armatureData);
		DBDataUtil.transformAnimationData(animationData, armatureData, isGlobalData);

		return animationData;
	}

	private static function parseSlotTimeline(timelineObject:Object, duration:int, frameRate:uint, isGlobalData:Boolean):SlotTimeline
	{
		var timeline:SlotTimeline = new SlotTimeline();
		timeline.name = timelineObject[ConstValues.A_NAME];
		timeline.scale = getNumber(timelineObject, ConstValues.A_SCALE, 1) || 0;
		timeline.offset = getNumber(timelineObject, ConstValues.A_OFFSET, 0) || 0;
		timeline.duration = duration;

		for each(var frameObject:Object in timelineObject[ConstValues.FRAME])
		{
			var frame:SlotFrame = parseSlotFrame(frameObject, frameRate, isGlobalData);
			timeline.addFrame(frame);
		}

		parseTimeline(timelineObject, timeline);

		return timeline;
	}

	private static function parseSlotFrame(frameObject:Object, frameRate:uint, isGlobalData:Boolean):SlotFrame
	{
		var frame:SlotFrame = new SlotFrame();
		parseFrame(frameObject, frame, frameRate);

		frame.visible = !getBoolean(frameObject, ConstValues.A_HIDE, false);

		//NaN:no tween, 10:auto tween, [-1, 0):ease in, 0:line easing, (0, 1]:ease out, (1, 2]:ease in out
		frame.tweenEasing = getNumber(frameObject, ConstValues.A_TWEEN_EASING, 10);
		frame.displayIndex = int(getNumber(frameObject,ConstValues.A_DISPLAY_INDEX,0));

		//如果为NaN，则说明没有改变过zOrder
		frame.zOrder = getNumber(frameObject, ConstValues.A_Z_ORDER, isGlobalData ? NaN:0);

		var colorTransformObject:Object = frameObject[ConstValues.COLOR_TRANSFORM];
		if(colorTransformObject)
		{
			frame.color = new ColorTransform();
			parseColorTransform(colorTransformObject, frame.color);
		}

		return frame;
	}

	private static function parseTransformTimeline(timelineObject:Object, duration:int, frameRate:uint, isGlobalData:Boolean):TransformTimeline
	{
		var timeline:TransformTimeline = new TransformTimeline();
		timeline.name = timelineObject[ConstValues.A_NAME];
		timeline.scale = getNumber(timelineObject, ConstValues.A_SCALE, 1) || 0;
		timeline.offset = getNumber(timelineObject, ConstValues.A_OFFSET, 0) || 0;
		timeline.originPivot.x = getNumber(timelineObject, ConstValues.A_PIVOT_X, 0) || 0;
		timeline.originPivot.y = getNumber(timelineObject, ConstValues.A_PIVOT_Y, 0) || 0;
		timeline.duration = duration;

		var frameObject:Object;
		var nextFrameObject:Object;

		for (var i:int = 0, len:int = timelineObject[ConstValues.FRAME].length; i < len; i++)
		{
			frameObject = timelineObject[ConstValues.FRAME][i];
			if (i < len -1)
			{
				nextFrameObject = timelineObject[ConstValues.FRAME][i + 1];
			}
			else if( i != 0)
			{
				nextFrameObject = timelineObject[ConstValues.FRAME][0];
			}
			else
			{
				nextFrameObject = null;
			}
			var frame:TransformFrame = parseTransformFrame(frameObject, nextFrameObject, frameRate, isGlobalData);
			timeline.addFrame(frame);
		}

		parseTimeline(timelineObject, timeline);

		return timeline;
	}

	private static function parseMainFrame(frameObject:Object, frameRate:uint):Frame
	{
		var frame:Frame = new Frame();
		parseFrame(frameObject, frame, frameRate);
		return frame;
	}

	private static function parseTransformFrame(frameObject:Object, nextFrameObject:Object, frameRate:uint, isGlobalData:Boolean):TransformFrame
	{
		var frame:TransformFrame = new TransformFrame();
		parseFrame(frameObject, frame, frameRate);

		frame.visible = !getBoolean(frameObject, ConstValues.A_HIDE, false);

		//NaN:no tween, 10:auto tween, [-1, 0):ease in, 0:line easing, (0, 1]:ease out, (1, 2]:ease in out
		frame.tweenEasing = getNumber(frameObject, ConstValues.A_TWEEN_EASING, 10);
		frame.tweenRotate = int(getNumber(frameObject, ConstValues.A_TWEEN_ROTATE, 0));
		frame.tweenScale = getBoolean(frameObject, ConstValues.A_TWEEN_SCALE, true);
		//frame.displayIndex = int(getNumber(frameObject, ConstValues.A_DISPLAY_INDEX, 0));

		if (nextFrameObject && int(getNumber(nextFrameObject, ConstValues.A_DISPLAY_INDEX, 0)) == -1)
		{
			frame.tweenEasing = NaN;
		}
		//如果为NaN，则说明没有改变过zOrder
		//frame.zOrder = getNumber(frameObject, ConstValues.A_Z_ORDER, isGlobalData ? NaN : 0);

		parseTransform(frameObject[ConstValues.TRANSFORM], frame.transform, frame.pivot);
		if(isGlobalData)//绝对数据
		{
			frame.global.copy(frame.transform);
		}

		frame.scaleOffset.x = getNumber(frameObject, ConstValues.A_SCALE_X_OFFSET, 0) || 0;
		frame.scaleOffset.y = getNumber(frameObject, ConstValues.A_SCALE_Y_OFFSET, 0) || 0;

		//var colorTransformObject:Object = frameObject[ConstValues.COLOR_TRANSFORM];
		//if(colorTransformObject)
		//{
			//frame.color = new ColorTransform();
			//parseColorTransform(colorTransformObject, frame.color);
		//}

		return frame;
	}

	private static function parseTimeline(timelineObject:Object, timeline:Timeline):void
	{
		var position:int = 0;
		var frame:Frame;
		for each(frame in timeline.frameList)
		{
			frame.position = position;
			position += frame.duration;
		}
		if(frame)
		{
			frame.duration = timeline.duration - frame.position;
		}
	}

	private static function parseFrame(frameObject:Object, frame:Frame, frameRate:uint):void
	{
		frame.duration = Math.round((Number(frameObject[ConstValues.A_DURATION]) || 1) * 1000 / frameRate);
		frame.action = frameObject[ConstValues.A_ACTION];
		frame.event = frameObject[ConstValues.A_EVENT];
		frame.sound = frameObject[ConstValues.A_SOUND];
	}

	private static function parseTransform(transformObject:Object, transform:DBTransform, pivot:Point = null):void
	{
		if(transformObject)
		{
			if(transform)
			{
				transform.x = getNumber(transformObject, ConstValues.A_X, 0) || 0;
				transform.y = getNumber(transformObject, ConstValues.A_Y, 0) || 0;
				transform.skewX = getNumber(transformObject, ConstValues.A_SKEW_X, 0) * ConstValues.ANGLE_TO_RADIAN || 0;
				transform.skewY = getNumber(transformObject, ConstValues.A_SKEW_Y, 0) * ConstValues.ANGLE_TO_RADIAN || 0;
				transform.scaleX = getNumber(transformObject, ConstValues.A_SCALE_X, 1) || 0;
				transform.scaleY = getNumber(transformObject, ConstValues.A_SCALE_Y, 1) || 0;
			}
			if(pivot)
			{
				pivot.x = getNumber(transformObject, ConstValues.A_PIVOT_X, 0) || 0;
				pivot.y = getNumber(transformObject, ConstValues.A_PIVOT_Y, 0) || 0;
			}
		}
	}

	private static function parseColorTransform(colorTransformObject:Object, colorTransform:ColorTransform):void
	{
		if(colorTransformObject)
		{
			if(colorTransform)
			{
				colorTransform.alphaOffset = int(colorTransformObject[ConstValues.A_ALPHA_OFFSET]);
				colorTransform.redOffset = int(colorTransformObject[ConstValues.A_RED_OFFSET]);
				colorTransform.greenOffset = int(colorTransformObject[ConstValues.A_GREEN_OFFSET]);
				colorTransform.blueOffset = int(colorTransformObject[ConstValues.A_BLUE_OFFSET]);

				colorTransform.alphaMultiplier = int(getNumber(colorTransformObject, ConstValues.A_ALPHA_MULTIPLIER,100)) * 0.01;
				colorTransform.redMultiplier = int(getNumber(colorTransformObject,ConstValues.A_RED_MULTIPLIER,100)) * 0.01;
				colorTransform.greenMultiplier = int(getNumber(colorTransformObject,ConstValues.A_GREEN_MULTIPLIER,100)) * 0.01;
				colorTransform.blueMultiplier = int(getNumber(colorTransformObject,ConstValues.A_BLUE_MULTIPLIER,100)) * 0.01;
			}
		}
	}

	private static function getBoolean(data:Object, key:String, defaultValue:Boolean):Boolean
	{
		if(data && key in data)
		{
			switch(String(data[key]))
			{
				case "0":
				case "NaN":
				case "":
				case "false":
				case "null":
				case "undefined":
					return false;

				case "1":
				case "true":
				default:
					return true;
			}
		}
		return defaultValue;
	}

	private static function getNumber(data:Object, key:String, defaultValue:Number):Number
	{
		if(data && key in data)
		{
			switch(String(data[key]))
			{
				case "NaN":
				case "":
				case "false":
				case "null":
				case "undefined":
					return NaN;

				default:
					return Number(data[key]);
			}
		}
		return defaultValue;
	}
}

