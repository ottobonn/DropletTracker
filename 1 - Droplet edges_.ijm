

originalID = getImageID();
originalTitle = getTitle();
saveDir = getDirectory("image");
getDimensions(width, height, channelCount, sliceCount, frameCount);

saveEachStep = false;
snapshotCounter = 1;

// Convert to 8-bit depth
run("8-bit");

// Find sudden changes in intensity
run("Find Edges", "stack");
if (saveEachStep){
    saveAs("Gif", saveDir + snapshotCounter + " - " + originalTitle + "-edges.gif");
    snapshotCounter++;
}

// Convert edges to binary mask
run("Make Binary", "method=Default background=Default calculate");
if (saveEachStep){
    saveAs("Gif", saveDir + snapshotCounter + " - " + originalTitle + "-binary.gif");
    snapshotCounter++;
}


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
if (saveEachStep){
    saveAs("Gif", saveDir + snapshotCounter + " - " + originalTitle + "-subtracted.gif");
    snapshotCounter++;
}


run("Make Binary", "method=Default background=Default");
if (saveEachStep){
    saveAs("Gif", saveDir + snapshotCounter + " - " + originalTitle + "-binary.gif");
    snapshotCounter++;
}

run("Despeckle", "stack");
if (saveEachStep){
    saveAs("Gif", saveDir + snapshotCounter + " - " + originalTitle + "-despeckled.gif");
    snapshotCounter++;
}

// Close up gaps twice
run("Close-", "stack");
if (saveEachStep){
    saveAs("Gif", saveDir + snapshotCounter + " - " + originalTitle + "-close1.gif");
    snapshotCounter++;
}
run("Close-", "stack");
if (saveEachStep){
    saveAs("Gif", saveDir + snapshotCounter + " - " + originalTitle + "-close2.gif");
    snapshotCounter++;
}

// Make borders bolder
run("Dilate", "stack");
if (saveEachStep){
    saveAs("Gif", saveDir + snapshotCounter + " - " + originalTitle + "-dilated1.gif");
    snapshotCounter++;
}

// Remove larger noise
run("Remove Outliers...", "radius=1 threshold=50 which=Dark stack");
if (saveEachStep){
    saveAs("Gif", saveDir + snapshotCounter + " - " + originalTitle + "-outliers5.gif");
    snapshotCounter++;
}

// Blur to close gaps
run("Gaussian Blur...", "sigma=3 stack");
if (saveEachStep){
    saveAs("Gif", saveDir + snapshotCounter + " - " + originalTitle + "-blur3.gif");
    snapshotCounter++;
}

run("Make Binary", "method=Default background=Default");
if (saveEachStep){
    saveAs("Gif", saveDir + snapshotCounter + " - " + originalTitle + "-binary.gif");
    snapshotCounter++;
}


run("Skeletonize", "stack");
if (saveEachStep){
    saveAs("Gif", saveDir + snapshotCounter + " - " + originalTitle + "-skeletonized.gif");
    snapshotCounter++;
}

// Fill drops
run("Fill Holes", "stack");
if (saveEachStep){
    saveAs("Gif", saveDir + snapshotCounter + " - " + originalTitle + "-fill.gif");
    snapshotCounter++;
}

// Clean up
run("Despeckle", "stack");
if (saveEachStep){
    saveAs("Gif", saveDir + snapshotCounter + " - " + originalTitle + "-despeckled.gif");
    snapshotCounter++;
}

run("Remove Outliers...", "radius=20 threshold=50 which=Dark stack");
if (saveEachStep){
    saveAs("Gif", saveDir + snapshotCounter + " - " + originalTitle + "-outliers20.gif");
    snapshotCounter++;
}

