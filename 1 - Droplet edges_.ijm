
originalID = getImageID();
getDimensions(width, height, channelCount, sliceCount, frameCount);

// Find sudden changes in intensity
run("Find Edges", "stack");

// Convert edges to binary mask
run("Make Binary", "method=Default background=Default calculate");

// Close up gaps
run("Close-", "stack");

// Fill drops
run("Fill Holes", "stack");

// Remove channel sidewalls
run("Z Project...", "start=1 stop=" + sliceCount + " projection=Median");
medianID = getImageID();

imageCalculator("Subtract create stack", originalID, medianID);
subtractResultID = getImageID;

selectImage(medianID);
close();

selectImage(originalID);
close();

selectImage(subtractResultID);

// Clean up
run("Despeckle", "stack");
run("Make Binary", "method=Default background=Default thresholded remaining white");
run("Remove Outliers...", "radius=20 threshold=50 which=Dark stack");
