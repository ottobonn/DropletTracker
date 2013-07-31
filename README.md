# DropletTracker
## A Droplet-Tracking Plugin for ImageJ

Travis Geis, SGTC, Stanford University, 2013.

DropletTracker is based heavily on MTrack2, by Nico Stuurman. It is releaased under the MIT license, 
and available at https://github.com/ottobonn/DropletTracker.

### Purpose

DropletTracker is actually a general-purpose particle tracker (it can track any binary blob on the image stack),
but with a specific focus on tracking microfluidic droplets in microchannel devices. It makes analyzing stacks 
of several thousands of frames a simple matter of time.

### Features

DropletTracker will track and record:

* Droplet centroid through time (X, Y)
* Area
* Perimeter
* Bounding rectangle
* Velocity
* Deformation parameter: a measure of roundness (or squareness) given by (width - height)/(width + height)

Output Fields:

* Particle Number: the number corresponding to the current droplet being tracked. Each droplet spans
    multiple rows of the spreadsheet, with each row being one "appearance" or "snapshot" of that droplet.
* Frame Number: the frame (slice) number wherein this droplet snapshot was taken. You can use the frame number
    to follow along in the original images and check the tracker output. Note that a droplet is considered
    "continuous" only if it is present in every frame along its track. If it disappears for one frame, it is a
    new droplet.
* X Centroid: The x location of the droplet centroid, in the units of your image. Set units in Analyze > Set Scale.
* Y Centroid: The y location of the droplet centroid, in the units of your image. Set units in Analyze > Set Scale.
* Area: the apparent area of the droplet, in the square units of the image.
* Perimeter: the perimeter of the droplet, in the image units.
* Bounds X: the X location of the corner of the bounding rectangle of the droplet, in the image units.
* Bounds Y: the Y location of the corner of the bounding rectangle of the droplet, in the image units.
* Bounds Width: the width of the bounding rectangle of the droplet (along the x axis) in the image units.
* Bounds Height: the height of the bounding rectangle of the droplet (along the y axis) in the image units.
* Velocity (units/frame): the apparent (frame-to-frame) velocity of the droplet, in image units per frame.
    This is the raw calculation that DropletTracker does on the images, and it is the measure to which the
    velocity cutoff applies. This value is unaffected by the framerate of the camera.
* Velocity (units/s): the actual (per second) velocity of the droplet, in image units per second. This velocity
    is given by the previous frame-to-frame velocity times the specified frame rate of the images. This is the
    real-world velocity of the droplet, and its accuracy depends on the accuracy with which you set the frame rate.
* Deformation Parameter: the unitless measure of circularity given by (width - height)/(width + height). These
    widths and heights are taken from the bounding rectangle, in the coordinate plane of the image. For rotating
    droplets, use Feret Deformation instead.
* Ambiguous Movement?: a yes-or-no indication of whether the identity of a drop between two successive frames is
    unclear. When two drop images appear near a drop within the distance given by max velocity, there is no way
    to be sure which of them corresponds to the original drop. This field will alert you if that is the case. You can
    reduce ambiguity by using a higher framerate, or setting a max velocity as close to the real velocity as possible.
    This field is blank unless it is true.
* Feret Length: the length of the longest axis of the droplet, in image units.
* Feret Width: the length of the shortest axis of the droplet, in image units. For an ellipse, this measure is orthogonal
    to the length, because it corresponds to the minor axis. **For non-ellipses, the Feret Length and Feret Width may not
    be orthogonal!**
* Feret Angle: the angle (in standard position, counter-clockwise from East) of the Feret major axis used to find the
    Feret Length. This is a good indicator of orientation for droplets that are not circular. Units are degrees.
* Feret Deformation: The deformation parameter (given by (width - height)/(width + height)) calculated using
    feret length and feret width. Note that for non-ellipses, this may be less meaningful, because **the Feret Length
    and Feret Width may not be orthogonal**.

### Details

DropletTracker tracks objects across stack frames using a nearest-neighbor approach. If an object in two
successive frames is within the user-specified "velocity" number of pixels, it is considered the same object as
in the previous frame. 

This approach creates ambiguity if two or more droplets are within reasonable range of a droplet in the
previous frame. In that case, the output will be "flagged," to indicate that the automated tracker could not
surely identify which objects were the same. The tracker does no emply any shape, size, or color heuristics,
because droplets often change size and shape as viewed from the camera, and many high-speed lab cameras are
black-and-white only.

For the best results and least ambiguity, use the highest-speed camera possible. Space between droplets should
be large compared to the distance travelled by one drop between frames. In addition, it helps to set the "maximum
velocity" as low as possible for your tracking situation. This velocity setting serves as a cutoff for how far
droplets might move between frames, so setting it excessively high will make every particle a candidate for one
particle in motion in the previous frame.

**Note**: The bounding rectangle is calculated with respect to the coordinate frame of the image. As a
result, the deformation parameter will only make sense for droplets aligned with the axis of the image.
If droplets are going around a corner, and their long axis is rotating, the deformation parameter will look
like it is changing even if the droplet does not deform, but instead rotates.

Instead, reference the "Feret" measurements for droplets that rotate. The Feret geometry rotates the frame
of reference with the long axis of the particle, so you can get an accurate idea of the deformation from
elliptical. The field "Feret Deformation" provides the same deformation parameter as above, but using the
Feret measures.

### Data Format

It's easy to export the results to file, and from there use any CSV-reading toolkit or application to do whatever
with the data. The output is a tab-delimeted table of values, with labeled headings as self-explanatory as possible.

When ImageJ shows the table of results, use File > Save to save them. Or, tick the "Save results file" box in the
DropletTracker popup to save results directly to a file of your choosing.

### Usage

DropletTracker is for tracking binary, black-on-white droplet blobs. The plugin takes a binary
(8-bit) stack and processes that. To get your raw images ready, some example macros are included in the
DropletTracker menu. Here's the workflow right now:

1.  Open the image stack you want to process.
2.  Set the image threshold level manually using Image>Adjust>Threshold...
Humans tend to be good at this step, so it isn't automatic yet.
3. Run the preprocessing macro: Plugins > DropletTracker > 1 - Enhance Droplets.
4. Check the output to make sure the droplets are clean, pure black blobs on a white background.
5. Run DropletTracker under Plugins > DropletTracker > DropletTracker
6. View and optionally save the resulting CSV. You may want to spot-check the values just to make
sure everything's on target.

Another preprocessing macro is almost ready, available under "1 - Droplet Edges." This new method uses
edge detection to combat uneven illumination.

### Installation

1. Grab the DropletTracker folder and drop it into your ImageJ / plugins directory.
2. Choose Plugins > Compile and Run... and select the DropletTracker_.java source file. 
3. Restart ImageJ. All done!

Find it under Plugins > DropletTracker.
