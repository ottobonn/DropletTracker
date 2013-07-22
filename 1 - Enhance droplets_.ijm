// This macro prepares a noisy raw image of 
// droplets for particle tracking plugins. It attempts to make
// solid dark droplets on a white background.

// How to use:
// 1. Open image stack containing your droplets
// 2. Set the threshold manually using Image>Adjust>Threshold...
// 3. Run this macro
// 4. Use your particle tracker of choice.

// Set the threshold manually before running this macro.
// This targets the dark outlines of droplets in a fluid channel.
// Your threshold should include as much of the drop outline as possible,
// even if the image is noisy as a result. The drop will be skeletonized,
// and so it needs a continuous perimeter to form a closed loop.

originalID = getImageID();
getDimensions(width, height, channelCount, sliceCount, frameCount);

selectImage(originalID);
setOption("BlackBackground", false);
run("Make Binary", "method=Default background=Default");

selectImage(originalID);
run("Z Project...", "start=1 stop=" + sliceCount + " projection=Median");
medianID = getImageID();

imageCalculator("Subtract create stack", originalID, medianID);
subtractResultID = getImageID;

selectImage(medianID);
close();

selectImage(originalID);
close();

selectImage(subtractResultID);

run("Despeckle", "stack");
run("Make Binary", "method=Default background=Default thresholded remaining white");
run("Remove Outliers...", "radius=5 threshold=50 which=Dark stack");
run("Dilate", "stack");
run("Skeletonize", "stack");
run("Fill Holes", "stack");
run("Remove Outliers...", "radius=7 threshold=50 which=Dark stack");
