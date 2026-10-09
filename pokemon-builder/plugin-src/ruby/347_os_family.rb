def os_family
  os = "other"
  case RUBY_PLATFORM
  when /ix/i, /ux/i, /gnu/i,/sysv/i, /solaris/i,/sunos/i, /bsd/i  
    os = "unix"
  when /win/i, /ming/i
    os = "windows"
  end
  return os
end
