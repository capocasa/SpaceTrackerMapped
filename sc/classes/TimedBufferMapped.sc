
// This adds support for recording mapped controls

RecordBufM {
  *ar {
    thisMethod.notYetImplemented;
  }

  *kr {
    arg inputArray, bufspec=#[0], run=1.0, doneAction=0;

    if (inputArray.size != bufspec.size) {
      RecordBufMError("inputArray size is %, but bufspec size is %, need be equal".format(inputArray.size, bufspec.size)).throw;
    };

    inputArray = inputArray.copy;

    bufspec.do{|bufspecch, i|
      var rec, inputch, run, time;
      inputch = inputArray[i].copy;
      run = inputch[0] > 0;
      time = Sweep.kr(1,run>0); 
      bufspecch[1..].pairsDo { |ch, b|
        RecordBufT.kr(inputch[ch], b, run:run);
        inputch[ch] = time;
      };
      inputArray[i] = inputch;
    };
    ^RecordBufT.kr(inputArray, bufspec.collect{|bufspecch|bufspecch[0]}, run, doneAction);
  }
}
RecordBufMError : Error {}

PlayBufM {
  *ar {
    thisMethod.notYetImplemented;
  }

  *kr {
    arg numChannels, bufspec = #[0], rate=1.0, trigger=1.0, startPos=0.0, doneAction=0;
    var play, out;

    out = PlayBufT.kr(numChannels, bufspec.collect{|bufspecch|bufspecch[0]}, rate, trigger, startPos, doneAction);

    bufspec.do { |bufspecch, i|
      var run;
      run = out[i][0] > 0;

      bufspecch[1..].pairsDo { |ch, b|
        var outch;
        outch = out[i].copy; // avoid buffer coloring error
        startPos = outch[ch];
        outch[ch] = PlayBufT.kr(1, b, rate * run, run, startPos) * run;
        out[i] = outch;
      };
    }
    ^out;
  }
}


+ Buffer {
  *allocMapped {
    arg server, polyphony, numChannels, channels = #[], frames = 16384, cframes = 16777216;
    var bufspec;
    bufspec = polyphony.collect {
      var bufspecch;
      bufspecch = Array.new(2*channels.size+1);
      bufspecch.add(Buffer.alloc(server, frames, numChannels+1));
      channels.do { |ch|
        bufspecch.add(ch);
        bufspecch.add(Buffer.alloc(server, cframes, 2));
      };
      bufspecch;
    };
    ^bufspec;
  }

  *readMapped {
    arg server, path;
    var bufspec, files, channels, buffers, sounds, polyphony, headerFormat, fileExtension;
    files=PathName(path).files.select{|p|p.fileName[0]==$p};
    #polyphony, channels, fileExtension = files.first.fileName.split($.);
    channels = channels.split($-).collect{|c|c.asInteger};
    headerFormat = fileExtension.toLower;
    headerFormat="aiff";
    polyphony = files.collect{|f|f.fileName[1..f.fileName.indexOf($.)-1].asInteger}.maxItem+1;
    sounds = files.collect{|f|SoundFile(f.fullPath)};
    bufspec=sounds.collect {|read|
      var write, line, spec, writeposch;
      "read %".format(read.path).postln;
      read.openRead;
      write = SoundFile(thisProcess.platform.defaultTempDir +/+ $m ++ 2147483647.rand ++ $.++ fileExtension);
      "write %".format(write.path).postln;
      write.numChannels = read.numChannels;
      write.sampleFormat = "float";
      write.headerFormat = headerFormat;
      write.openWrite;
      line = FloatArray.newClear(read.numChannels);
      spec = channels.collect {|ch|
        var writech;
        writech = SoundFile(thisProcess.platform.defaultTempDir +/+ $v ++ 2147483647.rand ++ $. ++ fileExtension);
        writech.headerFormat = headerFormat;
        writech.sampleFormat = "float";
        writech.openWrite;
        [ch, writech];
      }.flatten;

1.postln;
      writeposch = Array.fill(channels.maxItem+1, 0);
      while { read.readData(line); line.size > 0 }{
        var v, p, time;
"pre %".format(line.asCompileString).postln;
        if (line[1] > 0) {
2.postln;
          spec.pairsDo { |ch, writech, i|
            v = line[ch+1];
            p = path +/+ $v ++ v ++ $.++ fileExtension;

"channel path % exists %".format(p, File.exists(p)).postln;

            line[ch+1] = writeposch[ch];
3.postln;

            SoundFile.use(p) { |readch|
              var chunk;
              time = 0;
              chunk = FloatArray.newClear(16384);
              while { chunk.size > 0 }{
                readch.readData(chunk);
                writech.writeData(chunk);
                chunk.pairsDo {|t|time = time + t};
              };
              writeposch.atInc(ch, time);
              writech.close;
              //[\frames, frames, frames - readch.numFrames, readch.numFrames].postln;
            };
4.postln;
          };
        };
"post %".format(line.asCompileString).postln;
        write.writeData(line);
6.postln;
      };
      read.close;
      write.close;
      spec.pairsDo {|ch, writech, i|
        spec[i+1] = Buffer.readTimed(server, writech.path);
        fork {
          server.sync;
          File.delete(writech.path);
        };
      };
6.postln;
      [Buffer.readTimed(server, write.path)]++spec;
    };
    fork {
      server.sync;
      bufspec.collect{|b|File.delete(b[0].path)};
    };
7.postln;
    ^bufspec;
  }
}


+ Array {
  writeMapped {
    arg path, headerFormat = "aiff";
    var buffer, base, count, server, soundExtension, channels;
    server=this[0][0].server;
    soundExtension = headerFormat.toLower;
    count = 1;

//6.postln;
    //if (frames.asArray.every({|e|e==1})) {
    //  "No frames, not saving %".format(path).warn;
    //  this.yield;
    //};
    path.mkdir;
//5.postln;
    channels = [];
    this[0][1..].pairsDo{|ch|channels=channels.add(ch)};
    this.do { |bufspecch, i|
      var read, write, pathch, tmp, line, lastline, id;
      fork {
//4.postln;
        pathch=path+/+"p"++i++$.++channels.join("-")++$.++soundExtension;
        tmp = thisProcess.platform.defaultTempDir +/+ "map" ++ 2147483647.rand++$.++soundExtension;
        bufspecch[0].writeTimed(tmp, headerFormat);
        server.sync;
        read = SoundFile(tmp);
        read.openRead;
        write = SoundFile(pathch);
        write.numChannels = read.numChannels;
        write.headerFormat = headerFormat;
        write.sampleFormat = "float";
        write.openWrite;
        line = FloatArray.newClear(read.numChannels);
        lastline = Array.fill(line.size, 0);
//3.postln;
        id = 0;
        while { read.readData(line); lastline.size > 0 && { lastline[0] > 0 } } {
//2.postln;
          bufspecch[1..].pairsDo {|ch, b, x|
            var r, w, p, start, size;
            if (lastline[1] > 0) {  // Don't save signals for pause notes
//9.postln;
//line.postln;
              id = 16777215.rand; // largest integer that accurately casts to float
              start = lastline[ch+1]; // ch is output channel index, buffers have additional time channel at beginning, so ch+1
              size = start - line[ch+1];
              p = path +/+ "v"++id ++ $. ++ soundExtension;
//"read polyphony % id % of length % to % with value %".format(i,id,line[0],p,line[ch+1]).postln;
              b.writeTimed(p, headerFormat, size.asInteger, start.asInteger);
              line[ch+1] = id;
//"id %".format(id).postln;
            };
          };
//1.postln;
          lastline = line;
          write.writeData(line);
        };
        //server.sync; // Without this, longer passages crash the server
//7.postln;
        read.close;
        write.close;
        File.delete(tmp);
      };
//8.postln;

    }
  }
}

