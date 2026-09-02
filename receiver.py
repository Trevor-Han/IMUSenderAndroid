#!/usr/bin/env python3
"""IMU Sender PC receiver. Python 3, no third-party dependencies."""
import argparse, csv, json, socket, time

p = argparse.ArgumentParser()
p.add_argument('--port', type=int, default=5005)
p.add_argument('--bind', default='0.0.0.0')
p.add_argument('--csv', default='imu_data.csv')
args = p.parse_args()

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
sock.bind((args.bind, args.port))
print(f'Listening UDP on {args.bind}:{args.port}')
print('Set the Android app Computer IP to this PC LAN IP. Ctrl+C to stop.')

fields = ['timestamp','ax','ay','az','gx','gy','gz']
with open(args.csv, 'a', newline='', encoding='utf-8') as f:
    writer = csv.DictWriter(f, fieldnames=fields)
    if f.tell() == 0: writer.writeheader()
    try:
        while True:
            data, addr = sock.recvfrom(65535)
            try:
                obj = json.loads(data.decode('utf-8').strip())
                writer.writerow({k: obj.get(k, '') for k in fields}); f.flush()
                print(addr[0], ' '.join(f'{k}={obj.get(k)}' for k in fields[1:]))
            except Exception as e:
                print('Bad packet:', e, data[:100])
    except KeyboardInterrupt:
        print('\nSaved to', args.csv)
