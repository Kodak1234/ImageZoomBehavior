# ImageZoomBehavior
Android library that makes an imageview or imageButton zoomable

# Gestures supported

<ul>
<li>Pan</li>
<li>Zoom</li>
</ul>

Double tap too zoom in or zoom out is currently not supported.

# To implement double tab

Use these methods<br>

<strong>Note:</strong> Calling these methods may cause unintended effect.<br>

<strong>scale(float zoom)</strong>: Call from your code to zoom.<br>

<strong>toCenter()</strong>: Call to animate back to original position.<br>

<strong>updateFrame()</strong>: Always call this method when ever you change the size, scale, or the location of the view.

# Usage

add <strong>app:layout_behavior="yourPackage.ImageZoomBehavior" </strong> to the xml of your imageView.

