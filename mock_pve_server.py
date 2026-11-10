import http.server
import json
import sys

PORT = 18006

class MockPveHandler(http.server.BaseHTTPRequestHandler):
    def log_message(self, format, *args):
        pass # suppress default logging to stdout

    def do_POST(self):
        content_len = int(self.headers.get('Content-Length', 0))
        body = self.rfile.read(content_len).decode('utf-8') if content_len > 0 else ""

        if self.path == '/api2/json/access/ticket':
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            resp = {
                "data": {
                    "ticket": "PVE:root@pam:TESTTICKET123456789",
                    "CSRFPreventionToken": "TESTCSRF987654321",
                    "username": "root@pam",
                    "clustername": "pve-test"
                }
            }
            self.wfile.write(json.dumps(resp).encode('utf-8'))
        elif '/status/' in self.path:
            # Action execution (e.g. start, stop)
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            resp = {"data": "UPID:pve:00001:00002:00003:qmstart:100:root@pam:"}
            self.wfile.write(json.dumps(resp).encode('utf-8'))
        else:
            self.send_response(404)
            self.end_headers()

    def do_GET(self):
        auth_header = self.headers.get('Authorization', '')
        cookie_header = self.headers.get('Cookie', '')

        # Check authorization
        if not auth_header.startswith('PVEAPIToken=') and 'PVEAuthCookie=' not in cookie_header:
            self.send_response(401)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            self.wfile.write(json.dumps({"message": "authentication required"}).encode('utf-8'))
            return

        self.send_response(200)
        self.send_header('Content-Type', 'application/json')
        self.end_headers()

        if self.path == '/api2/json/version':
            resp = {"data": {"release": "8.2", "repoid": "test123", "version": "8.2.4"}}
        elif self.path == '/api2/json/nodes':
            resp = {"data": [
                {
                    "node": "pve-node-01",
                    "status": "online",
                    "cpu": 0.155,
                    "maxcpu": 8,
                    "mem": 8589934592,
                    "maxmem": 34359738368,
                    "disk": 25000000000,
                    "maxdisk": 100000000000,
                    "uptime": 86400
                }
            ]}
        elif '/status' in self.path:
            resp = {
                "data": {
                    "pveversion": "pve-manager/8.2.4/test123",
                    "kversion": "Linux 6.8.4-2-pve",
                    "uptime": 86400,
                    "cpu": 0.155,
                    "memory": {"total": 34359738368, "used": 8589934592, "free": 25769803776},
                    "rootfs": {"total": 100000000000, "used": 25000000000, "free": 75000000000},
                    "loadavg": [0.35, 0.20, 0.15]
                }
            }
        elif '/qemu' in self.path:
            resp = {"data": [
                {
                    "vmid": 100,
                    "name": "home-assistant",
                    "status": "running",
                    "cpu": 0.04,
                    "cpus": 2,
                    "maxcpu": 2,
                    "mem": 2147483648,
                    "maxmem": 4294967296,
                    "uptime": 43200,
                    "netin": 104857600,
                    "netout": 52428800,
                    "diskread": 1048576,
                    "diskwrite": 2097152
                }
            ]}
        elif '/lxc' in self.path:
            resp = {"data": [
                {
                    "vmid": 200,
                    "name": "nextcloud",
                    "status": "running",
                    "cpu": 0.02,
                    "cpus": 2,
                    "maxcpu": 2,
                    "mem": 1073741824,
                    "maxmem": 2147483648,
                    "swap": 0,
                    "maxswap": 1073741824,
                    "disk": 5000000000,
                    "maxdisk": 20000000000,
                    "uptime": 12000
                }
            ]}
        elif '/storage' in self.path:
            resp = {"data": [
                {
                    "storage": "local-zfs",
                    "type": "zfspool",
                    "active": 1,
                    "enabled": 1,
                    "used": 500000000000,
                    "total": 2000000000000,
                    "avail": 1500000000000,
                    "content": "images,rootdir"
                }
            ]}
        elif '/cluster/tasks' in self.path:
            resp = {"data": [
                {
                    "upid": "UPID:pve-node-01:0001:0002:0003:qmstart:100:root@pam:",
                    "node": "pve-node-01",
                    "user": "root@pam",
                    "type": "qmstart",
                    "status": "OK",
                    "starttime": 1700000000,
                    "endtime": 1700000005,
                    "id": "100"
                }
            ]}
        elif '/log' in self.path:
            resp = {"data": [
                {"n": 1, "t": "TASK START: starting VM 100"},
                {"n": 2, "t": "allocating memory: 4096 MB"},
                {"n": 3, "t": "VM 100 successfully started"},
                {"n": 4, "t": "TASK OK"}
            ]}
        else:
            resp = {"data": {}}

        self.wfile.write(json.dumps(resp).encode('utf-8'))

if __name__ == '__main__':
    server = http.server.HTTPServer(('127.0.0.1', PORT), MockPveHandler)
    server.serve_forever()
