# What is Virtual Copy And Paste ?

In this digital world with full of smart gadgets and computers, the ability of a new feature is evaluated based on how it is going to make user's life easier or better, rather than its complexity. One such great invention is Copy & Paste. This looks pretty minute but assume the life without Copy & Paste, which would be horrible.  

Why should we just limit it to a clipboard? Assume, what if we are able to copy the real-world objects from our physical world to digital world within a few clicks. The current idea is to extend the ability or scope of Copy & Paste to its next level.  

Virtual Copy & Paste is a way of copying text/objects from a physical world to digital world. We use a mobile app to capture the text/object from our surroundings and use the same app to paste the content on a digital screen.  


This is just a prototype to give the glimse of the idea.

## Modules

This prototype runs as 2 independent modules:

- **The mobile app**

  - Check out the [/android-app](/android-app) folder for instructions on how to deploy the app to your mobile.

- **The local server**

  - The interface between the mobile app and Photoshop.
  - It finds the position pointed on screen by the camera using [screenpoint](https://github.com/cyrildiagne/screenpoint)
  - Check out the [/server](/server) folder for instructions on configuring the local server

## Usage

### 1 - Configure Photoshop

- Go to "Preferences > Plug-ins", enable "Remote Connection" and set a friendly password that you'll need later.
- Make sure that your PS document settings match those in ```server/src/ps.py```, otherwise only an empty layer will be pasted.
- Find photoshop settings as follows: Goto Image Menu -> Select Image size ->
uncheck Scale styles and Constrain Proportions then change the values according to settings in ```server/src/ps.py```.


- Also make sure that your document has some sort of background. If the background is just blank, SIFT will probably not have enough feature to do a correct match.

### 2 - Configure and run the local server

- Follow the instructions in [/server](/server) to setup & run the local server.

### 3 - Configure and run the mobile app

- Follow the instructions in [/android-app](/android-app) to setup & deploy the mobile app.

## Thanks and Acknowledgements

- RunwayML for the [Photoshop paste code](https://github.com/runwayml/RunwayML-for-Photoshop/blob/master/host/index.jsx)