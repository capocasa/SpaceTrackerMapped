SpaceLinemapMapped : SpaceLinemap {

  var <>bufspec;

  *new {
    arg naming, bufspec;
    ^super.new(naming).bufspec_(bufspec).init;
  }

  mapSymbolic {
    arg line;
    line = super.mapSymbolic(line);
    ^line;
  }

  mapNumeric {
    arg line;
    line = super.mapNumeric(line);
    ^line;
  }

/*
    bufspec.do {|s, i|
      var base = path.dirname +/+ PathName(path).fileNameWithoutExtension;
      base.mkdir;
      s[1..].pairsDo { |j, b|
        var p = base +/+ i ++ $- ++ j ++ ".wav";
        b.write(p, "wav", "float");
      };
    };
*/

}

+ SpaceTracker {
  *toMapped {
    arg server, treefile;
    ^super.new.init(treefile).toMapped(server);
  }
  *fromMapped {
    arg treefile, bufspec, frames;
    ^super.new.init(treefile).fromMapped(bufspec, frames);
  }
  fromMapped {
  }
  toMapped {
  }
}

