# Secure
MODULE = "camel"

COMMANDS = ["camel:help"]

HELP = {
    "camel:help": "Show help for this module"
}

def execute(**kargs):
    command = kargs.get("command")
    app = kargs.get("app")
    args = kargs.get("args")
    env = kargs.get("env")

    if command == "camel:help":
        print "~ Print 'This is the help, which show nothing, because this module has no commands'" 
        print "~ "
        return
