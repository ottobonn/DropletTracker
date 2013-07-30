
originalID = getImageID();
getDimensions(width, height, channelCount, sliceCount, frameCount);

selectImage(originalID);
// Remove uneven lightfield
run("Bandpass Filter...", "filter_large=" + height/3 + " filter_small=0 suppress=Horizontal tolerance=5 process");

// Remove most background intensity
run("Subtract Background...", "rolling=50 light stack");
