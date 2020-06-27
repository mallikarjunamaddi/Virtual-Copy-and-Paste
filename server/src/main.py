import io
import os
from flask import Flask, request, jsonify, send_file
from flask_cors import CORS
from PIL import Image
import numpy as np
import time
import screenpoint
from datetime import datetime
import pyscreenshot
import requests
import logging
import argparse
import ps

logging.basicConfig(level=logging.INFO)

parser = argparse.ArgumentParser()
parser.add_argument('--photoshop_password', default='123456')
args = parser.parse_args()

max_view_size = 700
max_screenshot_size = 400

# Initialize the Flask application.
app = Flask(__name__)
CORS(app)


# Test Probe.
@app.route('/', methods=['GET'])
def hello():
    logging.info("get call")
    return 'Testing Virtual Copy & Paste.'

#Capture the Segmented Object, scale it and Save locally.
@app.route('/captureObject', methods=['POST'])
def captureObject():
    start = time.time()
    logging.info(' Capturing Object')

    if 'data' not in request.files:
        return jsonify({
            'status': 'error',
            'error': 'missing file param `data`'
        }), 400
    data = request.files['data'].read()
    if len(data) == 0:
        return jsonify({'status:': 'error', 'error': 'empty image'}), 400

    # Save captured_object locally.
    with open('captured_object.png', 'wb') as f:
        f.write(data)

    logging.info(' > opening captured_object...')
    captured_object = Image.open('captured_object.png')
    
    # Scale the captured_object.
    logging.info(' > scaling captured_object...')
    scaled_object = captured_object.resize((captured_object.size[0] * 3, captured_object.size[1] * 3))

    # Save scaled_object locally.
    logging.info(' > saving scaled_object...')
    scaled_object.save('scaled_object.png')

    # Print stats
    logging.info(f'Completed in {time.time() - start:.2f}s')

    # Return Status
    return jsonify({'status': 'ok'})


# The paste endpoints handles new paste requests.
@app.route('/paste', methods=['POST'])
def paste():
    start = time.time()
    logging.info(' PASTE')
    # Convert string of image data to uint8.
    if 'data' not in request.files:
        return jsonify({
            'status': 'error',
            'error': 'missing file param `data`'
        }), 400
    data = request.files['data'].read()
    if len(data) == 0:
        return jsonify({'status:': 'error', 'error': 'empty image'}), 400

    # Save debug locally.
    with open('view_image.jpg', 'wb') as f:
        f.write(data)

    # Convert string data to PIL Image.
    logging.info(' > loading image...')
    view = Image.open(io.BytesIO(data))

    # Ensure the view image size is under max_view_size.
    if view.size[0] > max_view_size or view.size[1] > max_view_size:
        view.thumbnail((max_view_size, max_view_size))

    # Take screenshot with pyscreenshot.
    logging.info(' > grabbing screenshot...')
    screen = pyscreenshot.grab()
    screen_width, screen_height = screen.size

    # Ensure screenshot is under max size.
    if screen.size[0] > max_screenshot_size or screen.size[1] > max_screenshot_size:
        screen.thumbnail((max_screenshot_size, max_screenshot_size))

    # Finds view centroid coordinates in screen space.
    logging.info(' > finding projected point...')
    view_arr = np.array(view.convert('L'))
    screen_arr = np.array(screen.convert('L'))
    # logging.info(f'{view_arr.shape}, {screen_arr.shape}')
    x, y = screenpoint.project(view_arr, screen_arr, False)
    logging.info(' > cordinates...')
    logging.info(x)
    logging.info(y)

    found = x != -1 and y != -1

    if found:
        # Bring back to screen space
        x = int(x / screen.size[0] * screen_width)
        y = int(y / screen.size[1] * screen_height)
        logging.info(f'{x}, {y}')

        # Paste the current image in photoshop at these coordinates.
        logging.info(' > sending to photoshop...')
        name = datetime.today().strftime('%Y-%m-%d-%H:%M:%S')
        img_path = os.path.join(os.getcwd(), 'scaled_object.png')
        err = ps.paste(img_path, name, x, y, password=args.photoshop_password)
        if err is not None:
            logging.error('error sending to photoshop')
            logging.error(err)
            jsonify({'status': 'error sending to photoshop'})
    else:
        logging.info('screen not found')

    # Print stats.
    logging.info(f'Completed in {time.time() - start:.2f}s')

    # Return status.
    if found:
        return jsonify({'status': 'ok'})
    else:
        return jsonify({'status': 'screen not found'})


if __name__ == '__main__':
    os.environ['FLASK_ENV'] = 'development'
    port = int(os.environ.get('PORT', 8080))
    app.run(debug=True, host='0.0.0.0', port=port)
