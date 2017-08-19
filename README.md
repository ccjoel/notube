# NoTube

## Installation

## Usage

Run the project directly:

    $ build.boot -h

Run the project's tests (they'll fail until you edit them):

    $ boot test

Build an uberjar from the project:

    $ boot build

Run the uberjar:

    $ java -jar target/notube-0.1.0.jar [args]
or,
    $ ./run.sh

## Options
```
  -t, --tokens ACTION                        Populate or refresh tokens. Action can be either p o r
  -n, --notube CHANNELID                     Scan and populate spam queue
  -r, --report                               Go through spam queue and report as spam
  -s, --search-channel CHANNEL-OR-USER-NAME  Search users by name, receive channel id
  -h, --help
```

## License

Copyright © 2017 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
