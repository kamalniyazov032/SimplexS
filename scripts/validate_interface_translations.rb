#!/usr/bin/env ruby
ROOT=File.expand_path("..",__dir__)
files=%w[az en ru].to_h do |locale|
  path=File.join(ROOT,"src/main/resources/messages_#{locale}.properties")
  data=File.readlines(path,encoding:"UTF-8").reject{|l|l.strip.empty?||l.start_with?("#","!")}.to_h{|line|line.chomp.split("=",2)}
  [locale,data]
end
source=files.fetch("az");errors=[]
files.each do |locale,data|
  errors<<"#{locale}: açar sayı #{data.size}, AZ #{source.size}" unless data.keys.sort==source.keys.sort
  data.each do |key,value|
    errors<<"#{locale}: boş dəyər #{key}" if value.nil?||value.strip.empty?
    expected=source[key].to_s.scan(/\{\d+\}/).sort;actual=value.to_s.scan(/\{\d+\}/).sort
    errors<<"#{locale}: parametr fərqi #{key}: #{expected} / #{actual}" unless expected==actual
  end
end
abort errors.join("\n") unless errors.empty?
puts "AZ/EN/RU: hər dildə #{source.size} açar, boş dəyər və parametr itkisi yoxdur."
