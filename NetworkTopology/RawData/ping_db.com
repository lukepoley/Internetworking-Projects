--- 2025-10-18_20-58 ---
PING db.com (34.98.101.189) 56(84) bytes of data.
64 bytes from 189.101.98.34.bc.googleusercontent.com (34.98.101.189): icmp_seq=1 ttl=118 time=15.8 ms
64 bytes from 189.101.98.34.bc.googleusercontent.com (34.98.101.189): icmp_seq=2 ttl=118 time=15.4 ms
64 bytes from 189.101.98.34.bc.googleusercontent.com (34.98.101.189): icmp_seq=3 ttl=118 time=15.8 ms
64 bytes from 189.101.98.34.bc.googleusercontent.com (34.98.101.189): icmp_seq=4 ttl=118 time=14.6 ms
64 bytes from 189.101.98.34.bc.googleusercontent.com (34.98.101.189): icmp_seq=5 ttl=118 time=16.6 ms
64 bytes from 189.101.98.34.bc.googleusercontent.com (34.98.101.189): icmp_seq=6 ttl=118 time=15.5 ms
64 bytes from 189.101.98.34.bc.googleusercontent.com (34.98.101.189): icmp_seq=7 ttl=118 time=15.6 ms
64 bytes from 189.101.98.34.bc.googleusercontent.com (34.98.101.189): icmp_seq=8 ttl=118 time=15.3 ms
64 bytes from 189.101.98.34.bc.googleusercontent.com (34.98.101.189): icmp_seq=9 ttl=118 time=14.3 ms
64 bytes from 189.101.98.34.bc.googleusercontent.com (34.98.101.189): icmp_seq=10 ttl=118 time=13.9 ms

--- db.com ping statistics ---
10 packets transmitted, 10 received, 0% packet loss, time 9013ms
rtt min/avg/max/mdev = 13.862/15.273/16.601/0.768 ms

--- Probe Info ---
Probe 1011101
Well-known Targets
facebook.com (v4)
Deviation:
0%
Avg:
13.62ms
Loss:
0%
google.com (v4)
Deviation:
0.28%
Avg:
12.21ms
Loss:
0%
wikipedia.org (v4)
Deviation:
1.39%
Avg:
12.64ms
Loss:
0%
