--- Traceroute from Sat Oct 18 10:42:56 AM CDT 2025 ---
traceroute to regex101.com (78.47.220.195), 30 hops max, 60 byte packets
 1  _gateway (10.133.0.254)  3.695 ms  3.664 ms  3.647 ms
 2  137.22.198.4 (137.22.198.4)  8.509 ms  10.827 ms  10.816 ms
 3  e0-21.switch3.msp1.he.net (184.105.39.113)  8.748 ms * *
 4  * port-channel22.core3.chi1.he.net (184.104.199.201)  17.846 ms *
 5  * * *
 6  * * port-channel11.core3.lon2.he.net (72.52.92.165)  125.445 ms
 7  100ge0-76.core1.lon3.he.net (184.104.198.245)  98.019 ms  98.738 ms  98.696 ms
 8  port-channel16.core1.ams2.he.net (184.105.65.17)  102.645 ms  103.687 ms  102.619 ms
 9  hetzner.interxionfra11.nl-ix.net (193.239.117.110)  109.045 ms  110.391 ms  110.211 ms
10  core11.nbg1.hetzner.com (213.239.224.233)  113.879 ms  113.753 ms  113.309 ms
11  * * *
12  * * *
13  27878.your-cloud.host (128.140.21.123)  113.139 ms  113.927 ms  112.905 ms
14  static.195.220.47.78.clients.your-server.de (78.47.220.195)  113.974 ms  112.978 ms  113.033 ms
