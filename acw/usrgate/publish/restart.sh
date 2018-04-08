set remote_ip [lindex $argv 0]
set pw [lindex $argv 1]
ssh root@$remote_ip

expect "password:"
send "$pw"
expect eof
