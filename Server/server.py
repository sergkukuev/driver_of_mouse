from bluetooth import *

FILE_NAME = '/sys/devices/platform/vms/coordinates'

server_sock=BluetoothSocket( RFCOMM )
server_sock.bind(("",1))
server_sock.listen(1)

port = server_sock.getsockname()[1]

uuid = "94f39d29-7d6d-437d-973b-fba39e49d4ee"

advertise_service( server_sock, "BluetoothServer",
                   service_id = uuid,
                   service_classes = [ uuid, SERIAL_PORT_CLASS ],
                   profiles = [ SERIAL_PORT_PROFILE ]
#                   ,protocols = [ OBEX_UUID ] 
                    )
                   
print("Waiting a client on RFCOMM channel %d" % port)

client_sock, client_info = server_sock.accept()
print("Accepted connection from ", client_info)

try:
	fd = os.open(FILE_NAME, os.O_TRUNC | os.O_WRONLY)
except FileNotFoundError:
	print('Error: No file to write coordinates')
	server_sock.close()

try:
    while True:
        data = client_sock.recv(1024)
        if len(data) == 0: break
        print("Received data: %s" % data)
        os.write(fd, data)
        #os.fsync(fd)
        if "</EOM>" in data: break
except IOError:
    pass

os.close(fd)
client_sock.send("The server will be turned off soon")
print("Disconnecting..")

client_sock.close()
server_sock.close()
print("All done disconnect")

def get_value_from_xml (string, tag):
    start = string.index("<"+tag+">")
    end = string.index("</"+tag+">", start)
    return string[start+len(tag)+2:end]

#print (data)
portion = get_value_from_xml(data, "portion")
#print (portion)
carbs = get_value_from_xml(portion, "carbs")
time_to_deliver = carbs * 10

import os
from time import sleep
serial = "584923"
port = "/dev/ttyUSB0"
directory = "/home/erobinson/diabetes/sus-res-test/decoding-carelink/bin"
command = "sudo python "+directory+"mm-set-suspend.py --serial "+serial+" --port "+port+" --verbose resume"
print(command)
#os.system(command)
sleep(time_to_deliver)
command = "sudo python "+directory+"mm-set-suspend.py --serial "+serial+" --port "+port+" --verbose suspend"
print(command)
#os.system(command)

print "all done"