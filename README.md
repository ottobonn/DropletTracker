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
like it is changing even if the droplet does not deform, but instead rotates. A future update might change this,
but right now the deformation is only valid for droplets aligned on the rectilinear axis of the image.

### Data Format

It's easy to export the results to file, and from there use any CSV-reading toolkit or application to do whatever
with the data. The output is a tab-delimeted table of values, with labeled headings as self-explanatory as possible.

### Usage

DropletTracker is for tracking binary, black-on-white droplets blobs. The plugin takes a binary
(8-bit) stack and processes that. To get your raw images ready, an example macro is included in the
DropletTracker menu entry. Here's the workflow right now:

1.  Open the image stack you want to process.
2.  Set the image threshold level manually using Image>Adjust>Threshold...
Humans tend to be good at this step, so it isn't automatic yet.
3. Run the preprocessing macro: Plugins > DropletTracker > 1 - Enhance Droplets.
4. Check the output to make sure the droplets are clean, pure black blobs on a white background.
5. Run DropletTracker under Plugins > DropletTracker > DropletTracker
6. View and optionally save the resulting CSV. You may want to spot-check the values just to make
sure everything's on target.

### Installation

1. Grab the DropletTracker folder and drop it into your ImageJ / plugins directory.
2. Choose Plugins > Compile and Run... and select the DropletTracker_.java source file. 
3. Restart ImageJ. All done!

Find it under Plugins > DropletTracker.
