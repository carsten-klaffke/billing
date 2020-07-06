
  Pod::Spec.new do |s|
    s.name = 'CapacitorBilling'
    s.version = '0.0.1'
    s.summary = 'Plugin for in-app-purchases'
    s.license = 'MIT'
    s.homepage = 'https://github.com/carsten-klaffke/billing.git'
    s.author = 'Carsten Klaffke'
    s.source = { :git => 'https://github.com/carsten-klaffke/billing.git', :tag => s.version.to_s }
    s.source_files = 'ios/Plugin/**/*.{swift,h,m,c,cc,mm,cpp}'
    s.ios.deployment_target  = '11.0'
    s.dependency 'Capacitor'
  end