# Virtual Copy & Paste local server

## Setup

```console
Install python 3.7.7 from https://www.python.org/downloads/release/python-377/
pip install virtualenv
virtualenv -p python3.7 venv
.\venv\Scripts\activate
pip install -r requirements.txt
```

## Run

Replace `123456` by your Photoshop remote connection password.

```console
python src/main.py --photoshop_password 123456
```

## Configuring and opening the port on Windows System:
Open Windows Defender Firewall with Advanced Security.
Write a new inbound rule for port 8080, give all types of acess to the port.