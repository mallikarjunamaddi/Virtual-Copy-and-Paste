# What is Virtual Copy And Paste ?

In this digital world with full of smart gadgets and computers, the ability of a new feature is evaluated based on how it is going to make user's life easier or better, rather than its complexity. One such great invention is Copy & Paste. This looks pretty minute but assume the life without Copy & Paste, which would be horrible.  

Why should we just limit it to a clipboard? Assume, what if we are able to copy the real-world objects from our physical world to digital world within a few clicks. The current idea is to extend the ability or scope of Copy & Paste to its next level.  

Virtual Copy & Paste is a way of copying text/objects from a physical world to digital world. We use a mobile app to capture the text/object from our surroundings and use the same app to paste the content on a digital screen.  

# Image Segmentation Android sample.

The used model, DeepLab
[https://ai.googleblog.com/2018/03/semantic-image-segmentation-with.html] is a
state-of-art deep learning model for semantic image segmentation, where the goal
is to assign semantic labels (e.g. person, dog, cat) to every pixel in the input
image.

## Requirements

*   Android Studio 3.2 (installed on a Linux, Mac or Windows machine)
*   An Android device, or an Android Emulator

## Build and run

### Step 1. Clone the source code

Clone the Git repository to your computer to get the demo
application.

```
git clone https://gitlab.com/mallikarjuna.maddi/virtual-copy-and-paste.git
```

### Step 2. Import the app to Android Studio

Open the source code in Android Studio. To do this, open Android
Studio and select `Import Projects (Gradle, Eclipse ADT, etc.)`

### Step 3. Run the Android app

Connect the Android device to the computer and be sure to approve any ADB
permission prompts that appear on your phone. Select `Run -> Run app.` Select
the deployment target in the connected devices to the device on which the app
will be installed. This will install the app on the device.

To test the app, open the app called `Virtual Copy and Paste` on your device.
Re-installing the app may require you to uninstall the previous installations.

## Resources used:

*   Camera2:
    https://developer.android.com/reference/android/hardware/camera2/package-summary
*   Camera2 base sample:
    https://github.com/android/camera-samples/tree/master/Camera2Formats
*   TensorFlow Lite: https://www.tensorflow.org/lite
*   TensorFlow Lite Image Segmentation Sample: 
    https://github.com/tensorflow/examples/tree/master/lite/examples/image_segmentation
*   ImageSegmentation model:
    https://www.tensorflow.org/lite/models/segmentation/overview
