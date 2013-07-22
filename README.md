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

### Data Format

It's easy to export the results to file, and from there use any CSV-reading toolkit or application to do whatever
with the data. The output is a tab-delimeted table of values, with labeled headings as self-explanatory as possible.
